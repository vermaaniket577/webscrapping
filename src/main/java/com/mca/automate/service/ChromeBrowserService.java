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
import java.net.URI;
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
                // Fallback: open in default browser anyway
                openInDefaultBrowser();
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
            log.info("No existing CDP endpoint on port {}. Launching browser with debugging enabled...", CDP_PORT);
            wsUrl = launchChromeWithDebugging();
        }

        if (wsUrl == null) {
            log.warn("Failed to obtain a CDP WebSocket URL. Falling back to default browser.");
            openInDefaultBrowser();
            return true;
        }

        log.info("✓ CDP WebSocket connected: {}", wsUrl);

        // ── Step 4 & 5: Inject cookies + localStorage, then navigate ──
        boolean result = injectAndNavigate(wsUrl, cookieMap, deviceId);
        if (!result) {
            log.warn("CDP injection failed. Falling back to default browser.");
            openInDefaultBrowser();
            return true;
        }
        return result;
    }

    /**
     * Opens MCA home page in the system's default browser.
     * Works with any browser: Chrome, Edge, Firefox, Brave, etc.
     */
    private void openInDefaultBrowser() {
        try {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new URI(MCA_HOME_URL));
                log.info("✓ MCA Home Page opened in default browser: {}", MCA_HOME_URL);
            } else {
                // Fallback for headless or unsupported Desktop environments
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", MCA_HOME_URL});
                log.info("✓ MCA Home Page opened via cmd start: {}", MCA_HOME_URL);
            }
        } catch (Exception e) {
            log.error("Failed to open MCA Home Page in default browser: {}", e.getMessage());
        }
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
        String url = "http://127.0.0.1:" + CDP_PORT + "/json/new";
        try {
            Request req = new Request.Builder().url(url).get().build();
            try (Response resp = httpClient.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) return null;
                JsonNode node = mapper.readTree(resp.body().string());
                String ws = node.path("webSocketDebuggerUrl").asText("");
                return ws.isBlank() ? null : ws;
            }
        } catch (Exception e) {
            log.debug("CDP endpoint not available: {}", e.getMessage());
            return null;
        }
    }

    private String launchChromeWithDebugging() {
        String chromePath = resolveChromePath();
        if (chromePath == null) {
            log.error("Could not find Chrome/Edge binary. Set 'chrome.binary.path' in application.yml.");
            return null;
        }

        try {
            // Use the user's DEFAULT browser profile so that if the browser is already
            // running, it opens a NEW TAB in the existing window instead of a new window.
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            String userDataDir = resolveDefaultProfileDir(chromePath, isWindows);
            
            if (userDataDir == null) {
                // Fallback to a temp profile if we can't find the default one
                userDataDir = System.getProperty("java.io.tmpdir") + "mca-chrome-profile";
            }
            new java.io.File(userDataDir).mkdirs();
            log.info("Using browser profile dir: {}", userDataDir);

            ProcessBuilder pb;
            
            if (isWindows) {
                pb = new ProcessBuilder(
                        "cmd.exe", "/c", "start", "\"\"",
                        "\"" + chromePath + "\"",
                        "--remote-debugging-port=" + CDP_PORT,
                        "--user-data-dir=" + userDataDir,
                        "--no-first-run",
                        "--no-default-browser-check",
                        "--disable-extensions",
                        "--start-maximized",
                        "about:blank"
                );
            } else {
                // Linux/AWS launch command
                pb = new ProcessBuilder(
                        chromePath,
                        "--remote-debugging-port=" + CDP_PORT,
                        "--user-data-dir=" + userDataDir,
                        "--no-first-run",
                        "--no-default-browser-check",
                        "--disable-extensions",
                        "--no-sandbox",
                        "--disable-dev-shm-usage",
                        "about:blank"
                );
            }
            
            pb.redirectErrorStream(true);
            pb.start();
            log.info("Browser process launched with user profile: {}", chromePath);

            return waitForCdpReady(10);
        } catch (Exception e) {
            log.error("Failed to launch Browser: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Resolves the user's default browser profile directory based on the browser binary path.
     * Using the default profile ensures that if the browser is already open,
     * it will open a new tab in the existing window rather than a separate window.
     */
    private String resolveDefaultProfileDir(String browserPath, boolean isWindows) {
        String userHome = System.getProperty("user.home");
        
        if (isWindows) {
            String lowerPath = browserPath.toLowerCase();
            if (lowerPath.contains("chrome")) {
                return userHome + "\\AppData\\Local\\Google\\Chrome\\User Data";
            } else if (lowerPath.contains("msedge") || lowerPath.contains("edge")) {
                return userHome + "\\AppData\\Local\\Microsoft\\Edge\\User Data";
            } else if (lowerPath.contains("brave")) {
                return userHome + "\\AppData\\Local\\BraveSoftware\\Brave-Browser\\User Data";
            }
        } else {
            // Linux
            String lowerPath = browserPath.toLowerCase();
            if (lowerPath.contains("chrome")) {
                return userHome + "/.config/google-chrome";
            } else if (lowerPath.contains("chromium")) {
                return userHome + "/.config/chromium";
            } else if (lowerPath.contains("brave")) {
                return userHome + "/.config/BraveSoftware/Brave-Browser";
            }
        }
        return null;
    }

    private String resolveChromePath() {
        if (chromeBinaryPath != null && !chromeBinaryPath.isBlank()) {
            File f = new File(chromeBinaryPath);
            if (f.exists()) return chromeBinaryPath;
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        
        List<String> candidates = new ArrayList<>();
        if (isWindows) {
            candidates.addAll(Arrays.asList(
                    System.getenv("ProgramFiles") + "\\Google\\Chrome\\Application\\chrome.exe",
                    System.getenv("ProgramFiles(x86)") + "\\Google\\Chrome\\Application\\chrome.exe",
                    System.getProperty("user.home") + "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe",
                    System.getenv("ProgramFiles") + "\\Microsoft\\Edge\\Application\\msedge.exe",
                    System.getenv("ProgramFiles(x86)") + "\\Microsoft\\Edge\\Application\\msedge.exe",
                    System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Edge\\Application\\msedge.exe",
                    System.getenv("ProgramFiles") + "\\BraveSoftware\\Brave-Browser\\Application\\brave.exe",
                    System.getProperty("user.home") + "\\AppData\\Local\\BraveSoftware\\Brave-Browser\\Application\\brave.exe"
            ));
        } else {
            // Linux/AWS Candidates
            candidates.addAll(Arrays.asList(
                    "/usr/bin/google-chrome",
                    "/usr/bin/google-chrome-stable",
                    "/usr/bin/chromium",
                    "/usr/bin/chromium-browser",
                    "/usr/bin/brave-browser"
            ));
        }

        for (String path : candidates) {
            if (path != null && new File(path).exists()) {
                log.info("Resolved browser binary at: {}", path);
                return path;
            }
        }
        log.warn("No Chromium-based browser found. Will fall back to system default browser.");
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

            private synchronized void sendNavigation(WebSocket webSocket) {
                if (navigationSent) return;
                navigationSent = true;
                log.info("✓ Navigating to MCA with session...");

                ObjectNode navCmd = mapper.createObjectNode();
                navCmd.put("id", msgId++);
                navCmd.put("method", "Page.navigate");
                ObjectNode navParams = mapper.createObjectNode();
                navParams.put("url", MCA_HOME_URL);
                navCmd.set("params", navParams);
                webSocket.send(navCmd.toString());

                ObjectNode bringCmd = mapper.createObjectNode();
                bringCmd.put("id", msgId++);
                bringCmd.put("method", "Page.bringToFront");
                webSocket.send(bringCmd.toString());

                try { Thread.sleep(2500); } catch (InterruptedException ignored) {}

                // Aggressively force Chrome to the foreground natively using a PowerShell COM object
                try {
                    boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
                    if (isWindows) {
                        String chromeBin = resolveChromePath();
                        if (chromeBin != null) {
                            String psCode = "$wshell = New-Object -ComObject wscript.shell; $wshell.AppActivate('MCA'); $wshell.AppActivate('Chrome');";
                            Runtime.getRuntime().exec(new String[]{"powershell.exe", "-Command", psCode});
                            log.info("✓ Executed native focus stealing commands for Windows.");
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to bring window to front natively: {}", e.getMessage());
                }

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

                // Fallback Navigation Timer (3.5 seconds)
                new Thread(() -> {
                    try {
                        Thread.sleep(3500);
                        if (!navigationSent) {
                            log.warn("Cookie confirmations delayed. Triggering fallback navigation...");
                            sendNavigation(webSocket);
                        }
                    } catch (InterruptedException ignored) {}
                }).start();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JsonNode msg = mapper.readTree(text);
                    if (msg.has("id")) {
                        int id = msg.path("id").asInt();
                        // If it's a response to one of the Network.setCookie commands (sent after Network.enable)
                        if (id >= 2 && id <= pendingCookies + 1) {
                            confirmedCookies++;
                            log.info("Cookie confirmation received {}/{}", confirmedCookies, pendingCookies);
                        }
                    }

                    if (confirmedCookies >= pendingCookies && pendingCookies > 0) {
                        sendNavigation(webSocket);
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
