package com.mca.automate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Opens Chrome with a temporary profile, injects MCA session cookies via Chrome
 * DevTools Protocol (CDP), and navigates to the MCA home page so the user lands
 * already logged in.
 *
 * <p>No extra dependencies required — uses OkHttp (already in the project) for
 * both the HTTP and WebSocket calls to the CDP endpoint.</p>
 */
@Service
public class ChromeBrowserService {

    private static final Logger log = LoggerFactory.getLogger(ChromeBrowserService.class);

    private static final String MCA_HOME_URL =
            "https://www.mca.gov.in/content/mca/global/en/home.html";
    private static final String MCA_DOMAIN = ".mca.gov.in";

    /** Fixed debugging port so we can reliably query the CDP endpoint. */
    private static final int CDP_PORT = 9222;

    private static final ObjectMapper mapper = new ObjectMapper();

    private final OkHttpClient wsClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    // ─── public entry point ──────────────────────────────────────────────

    public boolean openWithCookies(String cookieHeader) {
        try {
            // 1. Try connecting to an existing Chrome first
            String wsUrl = getWebSocketDebugUrl(1);
            
            if (wsUrl != null) {
                log.info("+++++ Found existing Chrome on CDP port 9222");
            } else {
                // 2. If no existing Chrome found, try launching it
                log.info("+++++ No existing Chrome found on 9222. Launching new instance...");
                String chromePath = findChromePath();
                if (chromePath == null) {
                    log.error("+++++ Chrome executable not found. Cannot open browser with cookies.");
                    return false;
                }

                // Create a temporary user-data-dir so we don't interfere with the user's default Chrome profile.
                Path tempProfile = Files.createTempDirectory("mca_chrome_");
                log.info("+++++ Chrome temp profile: {}", tempProfile);

                // Launch Chrome
                ProcessBuilder pb = new ProcessBuilder(
                        chromePath,
                        "--remote-debugging-port=" + CDP_PORT,
                        "--user-data-dir=" + tempProfile.toAbsolutePath(),
                        "--no-first-run",
                        "--no-default-browser-check",
                        "about:blank"
                );
                pb.redirectErrorStream(true);
                Process chromeProcess = pb.start();

                // Give Chrome a moment to bind the debugging port
                Thread.sleep(2000);

                // Verify Chrome is alive
                if (!chromeProcess.isAlive()) {
                    log.error("+++++ Chrome process exited immediately. Exit code: {}", chromeProcess.exitValue());
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(chromeProcess.getInputStream()))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            log.error("+++++ Chrome output: {}", line);
                        }
                    }
                    return false;
                }

                wsUrl = getWebSocketDebugUrl(3);
                if (wsUrl == null) {
                    log.error("+++++ Could not obtain Chrome CDP WebSocket URL");
                    return false;
                }
            }

            log.info("+++++ CDP WebSocket URL: {}", wsUrl);

            // Parse cookies and inject via CDP, then navigate
            List<CookiePair> cookies = parseCookies(cookieHeader);
            injectCookiesAndNavigate(wsUrl, cookies);

            log.info("+++++ MCA Home Page opened in Chrome with {} session cookies", cookies.size());
            return true;

        } catch (Exception e) {
            log.error("+++++ Failed to open Chrome with cookies: {}", e.getMessage(), e);
            return false;
        }
    }

    // ─── Chrome path detection ───────────────────────────────────────────

    private String findChromePath() {
        List<String> candidates = new ArrayList<>();

        // Windows paths
        String programFiles = System.getenv("ProgramFiles");
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        String localAppData = System.getenv("LOCALAPPDATA");

        if (programFiles != null) {
            candidates.add(programFiles + "\\Google\\Chrome\\Application\\chrome.exe");
        }
        if (programFilesX86 != null) {
            candidates.add(programFilesX86 + "\\Google\\Chrome\\Application\\chrome.exe");
        }
        if (localAppData != null) {
            candidates.add(localAppData + "\\Google\\Chrome\\Application\\chrome.exe");
        }

        // Linux / macOS fallbacks
        candidates.add("/usr/bin/google-chrome");
        candidates.add("/usr/bin/google-chrome-stable");
        candidates.add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");

        for (String path : candidates) {
            if (new File(path).exists()) {
                log.info("+++++ Found Chrome at: {}", path);
                return path;
            }
        }
        return null;
    }

    // ─── CDP helpers ─────────────────────────────────────────────────────

    /**
     * Queries {@code http://localhost:<port>/json} to find the target tab.
     * Prefers the tab that contains 'localhost', otherwise returns the first page.
     */
    private String getWebSocketDebugUrl(int maxRetries) {
        String url = "http://localhost:" + CDP_PORT + "/json";

        for (int i = 0; i < maxRetries; i++) {
            try {
                Request req = new Request.Builder().url(url).get().build();
                try (Response resp = wsClient.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        String body = resp.body().string();
                        JsonNode pages = mapper.readTree(body);
                        if (pages.isArray()) {
                            String firstWsUrl = null;
                            for (JsonNode page : pages) {
                                String type = page.path("type").asText("");
                                String pageUrl = page.path("url").asText("");
                                String ws = page.path("webSocketDebuggerUrl").asText(null);
                                
                                if ("page".equals(type) && ws != null) {
                                    if (firstWsUrl == null) firstWsUrl = ws;
                                    // Prefer the tab where the user is currently on our app
                                    if (pageUrl.contains("localhost") || pageUrl.contains("127.0.0.1")) {
                                        return ws;
                                    }
                                }
                            }
                            if (firstWsUrl != null) return firstWsUrl;
                        }
                    }
                }
            } catch (Exception ignored) {
                // Chrome may not be ready yet
            }
            if (i < maxRetries - 1) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }
        return null;
    }

    /**
     * Connects to the CDP WebSocket, sends {@code Network.setCookie} for every
     * cookie, then sends {@code Page.navigate} to the MCA home URL, and finally
     * sets localStorage/sessionStorage using {@code Runtime.evaluate}.
     */
    private void injectCookiesAndNavigate(String wsUrl, List<CookiePair> cookies) throws Exception {
        CountDownLatch doneLatch = new CountDownLatch(1);
        AtomicInteger msgId = new AtomicInteger(1);
        
        // Extract required values for local/session storage
        String sessionId = "";
        String sessionMd5 = "";
        String deviceId = "";
        
        for (CookiePair cookie : cookies) {
            if ("sessionID".equals(cookie.name)) sessionId = cookie.value;
            if ("session-token-md5".equals(cookie.name)) sessionMd5 = cookie.value;
            if ("deviceId".equals(cookie.name)) deviceId = cookie.value;
        }
        
        final String fSessionId = sessionId;
        final String fSessionMd5 = sessionMd5;
        final String fDeviceId = deviceId;

        Request wsRequest = new Request.Builder().url(wsUrl).build();
        WebSocket ws = wsClient.newWebSocket(wsRequest, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                // First enable the Network and Page domains
                int enableId = msgId.getAndIncrement();
                webSocket.send("{\"id\":" + enableId + ",\"method\":\"Network.enable\"}");
                int pageEnableId = msgId.getAndIncrement();
                webSocket.send("{\"id\":" + pageEnableId + ",\"method\":\"Page.enable\"}");

                // Set each cookie
                for (CookiePair cookie : cookies) {
                    int id = msgId.getAndIncrement();
                    String cmd = String.format(
                            "{\"id\":%d,\"method\":\"Network.setCookie\",\"params\":"
                                    + "{\"name\":\"%s\",\"value\":\"%s\","
                                    + "\"domain\":\"%s\",\"path\":\"/\","
                                    + "\"secure\":true,\"httpOnly\":true}}",
                            id,
                            escapeJson(cookie.name),
                            escapeJson(cookie.value),
                            MCA_DOMAIN
                    );
                    webSocket.send(cmd);
                    log.info("+++++ Set cookie: {}={}", cookie.name,
                            cookie.value.length() > 20
                                    ? cookie.value.substring(0, 20) + "..."
                                    : cookie.value);
                }

                // Add script to evaluate on new document to set localStorage/sessionStorage in the MCA domain
                String js = String.format(
                    "localStorage.setItem('sessionID', '%s'); " +
                    "localStorage.setItem('session-token-md5', '%s'); " +
                    "localStorage.setItem('deviceId', '%s'); " +
                    "sessionStorage.setItem('sessionID', '%s'); " +
                    "sessionStorage.setItem('session-token-md5', '%s'); " +
                    "sessionStorage.setItem('deviceId', '%s');",
                    escapeJson(fSessionId), escapeJson(fSessionMd5), escapeJson(fDeviceId),
                    escapeJson(fSessionId), escapeJson(fSessionMd5), escapeJson(fDeviceId)
                );
                
                int scriptId = msgId.getAndIncrement();
                String scriptCmd = String.format(
                        "{\"id\":%d,\"method\":\"Page.addScriptToEvaluateOnNewDocument\",\"params\":{\"source\":\"%s\"}}",
                        scriptId, escapeJson(js)
                );
                webSocket.send(scriptCmd);
                log.info("+++++ Sent Page.addScriptToEvaluateOnNewDocument to set localStorage/sessionStorage");

                // Navigate to MCA home page
                int navId = msgId.getAndIncrement();
                String navCmd = String.format(
                        "{\"id\":%d,\"method\":\"Page.navigate\",\"params\":{\"url\":\"%s\"}}",
                        navId, MCA_HOME_URL
                );
                webSocket.send(navCmd);
                log.info("+++++ Sent Page.navigate to {}", MCA_HOME_URL);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JsonNode msg = mapper.readTree(text);
                    // Look for the Page.navigate response (it'll have a frameId in result)
                    if (msg.has("result") && msg.path("result").has("frameId")) {
                        log.info("+++++ Page.navigate response received — navigation started");
                        doneLatch.countDown();
                    }
                } catch (Exception e) {
                    log.warn("+++++ CDP message parse error: {}", e.getMessage());
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("+++++ CDP WebSocket failure: {}", t.getMessage());
                doneLatch.countDown();
            }
        });

        // Wait for navigation to complete (max 10 seconds)
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        if (!completed) {
            log.warn("+++++ CDP commands timed out, but cookies may still have been set");
        }

        // Close WebSocket gracefully — Chrome stays open for the user
        ws.close(1000, "done");
    }

    // ─── cookie parsing ──────────────────────────────────────────────────

    private List<CookiePair> parseCookies(String cookieHeader) {
        List<CookiePair> result = new ArrayList<>();
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return result;
        }
        for (String part : cookieHeader.split(";")) {
            String trimmed = part.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0) {
                String name = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                result.add(new CookiePair(name, value));
            }
        }
        return result;
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private record CookiePair(String name, String value) {}
}
