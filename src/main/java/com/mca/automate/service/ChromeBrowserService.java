package com.mca.automate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mca.automate.dto.VerifyOtpDTO;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ChromeBrowserService {

    private static final int CDP_PORT = 9222;
    private static final String MCA_HOME_URL = "https://www.mca.gov.in/content/mca/global/en/home.html";
    private static final String MCA_DOMAIN = ".mca.gov.in";

    /** Required cookie keys that MUST be present for a valid MCA session */
    private static final Set<String> REQUIRED_COOKIES = Set.of("sessionID", "session-token-md5");

    @Value("${chrome.binary.path:}")
    private String chromeBinaryPath;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public boolean openWithCookies(String finalCookie, VerifyOtpDTO dto) {

        // ── Step 1: Validate credentials ──────────────────────
        Map<String, String> cookieMap = parseCookies(finalCookie);

        for (String required : REQUIRED_COOKIES) {
            String val = cookieMap.get(required);
            if (val == null || val.isBlank()) {
                log.error("Missing required cookie '{}'. Cannot open MCA. Available cookies: {}", required, cookieMap.keySet());
                return false;
            }
        }

        String deviceId = dto.getDeviceId();
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = cookieMap.getOrDefault("deviceId", "");
        }
        if (deviceId.isBlank()) {
            log.warn("deviceId is missing; MCA session may be incomplete.");
        }

        log.info("✓ Credential validation passed. sessionID={}, session-token-md5 length={}, deviceId={}",
                cookieMap.get("sessionID").substring(0, Math.min(8, cookieMap.get("sessionID").length())) + "...",
                cookieMap.get("session-token-md5").length(),
                deviceId.substring(0, Math.min(8, deviceId.length())) + "...");

        // ── Step 2: Try existing CDP endpoint (current browser) ──
        String wsUrl = findAnyTabWsUrl();

        // ── Step 3: If no CDP found, launch Chrome with debugging ──
        if (wsUrl == null) {
            log.info("No existing CDP endpoint on port {}. Launching Chrome with debugging enabled...", CDP_PORT);
            wsUrl = launchChromeWithDebugging();
        }

        if (wsUrl == null) {
            log.error("Failed to obtain a CDP WebSocket URL. Cannot inject cookies.");
            return false;
        }

        log.info("✓ CDP WebSocket connected: {}", wsUrl);

        // ── Step 4 & 5: Inject cookies + localStorage, then navigate ──
        return injectAndNavigate(wsUrl, cookieMap, deviceId);
    }

    private Map<String, String> parseCookies(String cookieStr) {
        Map<String, String> map = new LinkedHashMap<>();
        if (cookieStr == null || cookieStr.isBlank()) return map;

        for (String part : cookieStr.split(";")) {
            String trimmed = part.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0) {
                String name = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                map.put(name, value);
            }
        }
        return map;
    }

    private String findAnyTabWsUrl() {
        String url = "http://127.0.0.1:" + CDP_PORT + "/json";
        try {
            Request req = new Request.Builder().url(url).get().build();
            try (Response resp = httpClient.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) return null;

                JsonNode nodes = mapper.readTree(resp.body().string());
                if (!nodes.isArray()) return null;

                String localhostWs = null;
                String anyPageWs = null;

                for (JsonNode node : nodes) {
                    if (!"page".equals(node.path("type").asText())) continue;
                    String pageUrl = node.path("url").asText("");
                    String ws = node.path("webSocketDebuggerUrl").asText("");

                    if (pageUrl.contains("localhost:8082") || pageUrl.contains("127.0.0.1:8082")) {
                        localhostWs = ws;
                        break; 
                    }
                    if (anyPageWs == null && !ws.isBlank()) {
                        anyPageWs = ws;
                    }
                }

                return localhostWs != null ? localhostWs : anyPageWs;
            }
        } catch (Exception e) {
            log.debug("CDP endpoint not available: {}", e.getMessage());
            return null;
        }
    }

    private String launchChromeWithDebugging() {
        String chromePath = resolveChromePath();
        if (chromePath == null) {
            log.error("Could not find Chrome binary. Set 'chrome.binary.path' in application.yml.");
            return null;
        }

        try {
            String tempDir = System.getProperty("java.io.tmpdir") + "mca-chrome-profile";
            new java.io.File(tempDir).mkdirs();
            log.info("Using Chrome profile dir: {}", tempDir);

            ProcessBuilder pb = new ProcessBuilder(
                    chromePath,
                    "--remote-debugging-port=" + CDP_PORT,
                    "--user-data-dir=" + tempDir,
                    "--no-first-run",
                    "--no-default-browser-check",
                    "--disable-extensions",
                    "about:blank"
            );
            pb.redirectErrorStream(true);
            pb.start();
            log.info("Chrome process launched with dedicated profile: {}", chromePath);

            return waitForCdpReady(10);
        } catch (Exception e) {
            log.error("Failed to launch Chrome: {}", e.getMessage());
            return null;
        }
    }

    private String resolveChromePath() {
        if (chromeBinaryPath != null && !chromeBinaryPath.isBlank()) {
            File f = new File(chromeBinaryPath);
            if (f.exists()) return chromeBinaryPath;
        }

        String[] candidates = {
                System.getenv("ProgramFiles") + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getenv("ProgramFiles(x86)") + "\\Google\\Chrome\\Application\\chrome.exe",
                System.getProperty("user.home") + "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe"
        };
        for (String path : candidates) {
            if (path != null && new File(path).exists()) {
                log.info("Resolved Chrome binary at: {}", path);
                return path;
            }
        }
        return null;
    }

    private String waitForCdpReady(int maxWaitSeconds) {
        for (int i = 0; i < maxWaitSeconds * 2; i++) {
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }

            String url = "http://127.0.0.1:" + CDP_PORT + "/json";
            try {
                Request req = new Request.Builder().url(url).get().build();
                try (Response resp = httpClient.newCall(req).execute()) {
                    if (resp.isSuccessful() && resp.body() != null) {
                        JsonNode nodes = mapper.readTree(resp.body().string());
                        if (nodes.isArray()) {
                            for (JsonNode node : nodes) {
                                if ("page".equals(node.path("type").asText())) {
                                    String ws = node.path("webSocketDebuggerUrl").asText("");
                                    if (!ws.isBlank()) {
                                        log.info("CDP ready after {}ms", (i + 1) * 500);
                                        return ws;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        log.error("CDP did not become ready within {}s", maxWaitSeconds);
        return null;
    }

    private boolean injectAndNavigate(String wsUrl, Map<String, String> cookieMap, String deviceId) {
        CountDownLatch latch = new CountDownLatch(1);
        boolean[] success = {false};

        Request request = new Request.Builder().url(wsUrl).build();
        WebSocket ws = httpClient.newWebSocket(request, new WebSocketListener() {
            int msgId = 1;
            int pendingCookies = 0;
            int confirmedCookies = 0;
            boolean navigationSent = false;

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("CDP WebSocket connected. Injecting {} cookies BEFORE loading MCA...", cookieMap.size());

                ObjectNode enableNetwork = mapper.createObjectNode();
                enableNetwork.put("id", msgId++);
                enableNetwork.put("method", "Network.enable");
                webSocket.send(enableNetwork.toString());

                for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
                    ObjectNode params = mapper.createObjectNode();
                    params.put("name", entry.getKey());
                    params.put("value", entry.getValue());
                    params.put("domain", MCA_DOMAIN);
                    params.put("path", "/");
                    params.put("url", "https://www.mca.gov.in");
                    params.put("secure", true);
                    params.put("httpOnly", isHttpOnlyCookie(entry.getKey()));

                    ObjectNode cmd = mapper.createObjectNode();
                    cmd.put("id", msgId++);
                    cmd.put("method", "Network.setCookie");
                    cmd.set("params", params);
                    webSocket.send(cmd.toString());
                    pendingCookies++;

                    log.info("  → Setting cookie: {}={}", entry.getKey(),
                            entry.getValue().length() > 20 ? entry.getValue().substring(0, 20) + "..." : entry.getValue());
                }

                log.info("All {} cookies sent. Waiting for confirmations before navigating...", pendingCookies);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JsonNode msg = mapper.readTree(text);
                    if (msg.has("result") && msg.path("result").has("success")) {
                        boolean cookieSet = msg.path("result").path("success").asBoolean(false);
                        if (cookieSet) confirmedCookies++;
                        log.debug("Cookie confirmation {}/{}", confirmedCookies, pendingCookies);
                    }

                    if (confirmedCookies >= pendingCookies && pendingCookies > 0 && !navigationSent) {
                        navigationSent = true;
                        log.info("✓ All {} cookies confirmed. NOW navigating to MCA with session...", confirmedCookies);

                        ObjectNode navCmd = mapper.createObjectNode();
                        navCmd.put("id", msgId++);
                        navCmd.put("method", "Page.navigate");
                        ObjectNode navParams = mapper.createObjectNode();
                        navParams.put("url", MCA_HOME_URL);
                        navCmd.set("params", navParams);
                        webSocket.send(navCmd.toString());

                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

                        String safeSessionId = escape(cookieMap.getOrDefault("sessionID", ""));
                        String safeSessionMd5 = escape(cookieMap.getOrDefault("session-token-md5", ""));
                        String safeDeviceId = escape(deviceId);

                        String js = String.format(
                                "try { " +
                                "  localStorage.setItem('sessionID', '%s'); " +
                                "  localStorage.setItem('session-token-md5', '%s'); " +
                                "  localStorage.setItem('deviceId', '%s'); " +
                                "  sessionStorage.setItem('sessionID', '%s'); " +
                                "  sessionStorage.setItem('session-token-md5', '%s'); " +
                                "  sessionStorage.setItem('deviceId', '%s'); " +
                                "  'OK'; " +
                                "} catch(e) { e.message; }",
                                safeSessionId, safeSessionMd5, safeDeviceId,
                                safeSessionId, safeSessionMd5, safeDeviceId
                        );

                        ObjectNode evalParams = mapper.createObjectNode();
                        evalParams.put("expression", js);
                        ObjectNode evalCmd = mapper.createObjectNode();
                        evalCmd.put("id", msgId++);
                        evalCmd.put("method", "Runtime.evaluate");
                        evalCmd.set("params", evalParams);
                        webSocket.send(evalCmd.toString());

                        log.info("✓ MCA page navigated with cookies. localStorage injected.");
                        success[0] = true;
                        latch.countDown();
                    }
                } catch (Exception e) {
                    log.debug("CDP message parse error: {}", e.getMessage());
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                latch.countDown();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("CDP WebSocket failure: {}", t.getMessage());
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(15, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("CDP injection timed out after 15s. Cookies may have been partially set.");
                success[0] = true; 
            }
            ws.close(1000, "Done");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (success[0]) {
            log.info("✓ MCA portal opened with authenticated session.");
        }
        return success[0];
    }

    private boolean isHttpOnlyCookie(String name) {
        return Set.of("sessionID", "session-token-md5", "bm_sv", "ak_bmsc", "_csrf").contains(name);
    }

    private String escape(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
