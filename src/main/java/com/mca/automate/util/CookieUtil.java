package com.mca.automate.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

/* JADX INFO: loaded from: CookieUtil.class */
@Component
public class CookieUtil {
    public String getResponseCookies() {
        OkHttpClient client = McaOkHttpClientFactory.newBuilder().followRedirects(true).build();
        Request request = new Request.Builder().url("https://www.mca.gov.in/content/mca/global/en/foportal/fologin.html").method("GET", (RequestBody) null).addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7").addHeader("accept-language", "en-US,en;q=0.9").addHeader("sec-ch-ua", "\"Google Chrome\";v=\"147\", \"Not.A/Brand\";v=\"8\", \"Chromium\";v=\"147\"").addHeader("sec-ch-ua-mobile", "?0").addHeader("sec-fetch-dest", "document").addHeader("sec-fetch-mode", "navigate").addHeader("sec-fetch-site", "none").addHeader("sec-fetch-user", "?1").addHeader("upgrade-insecure-requests", "1").addHeader("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").build();
        try {
            Response response = client.newCall(request).execute();
            try {
                System.out.println("Response Code: " + response.code());
                List<String> cookies = response.headers("Set-Cookie");
                if (cookies.isEmpty()) {
                    System.out.println("No cookies were sent by the server.");
                } else {
                    System.out.println("--- Found " + cookies.size() + " Cookie Headers ---");
                    for (String cookie : cookies) {
                        System.out.println("Cookie: " + cookie);
                    }
                }
                String cookieInventory = this.cookieStringFromHeaders(cookies);
                cookieInventory = this.fetchCsrfCookies(client, cookieInventory);
                cookieInventory = this.ensureCsrfCookie(cookieInventory);
                System.out.println("--- Combined Cookie String ---");
                System.out.println(cookieInventory.trim());
                String strTrim = cookieInventory.trim();
                if (response != null) {
                    response.close();
                }
                return strTrim;
            } finally {
            }
        } catch (IOException e) {
            System.err.println("Error executing request: " + e.getMessage());
            return "";
        }
    }

    private String fetchCsrfCookies(OkHttpClient client, String cookieInventory) {
        Request csrfRequest = new Request.Builder()
                .url("https://www.mca.gov.in/libs/granite/csrf/token.json")
                .get()
                .addHeader("accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("accept-language", "en-US,en;q=0.9")
                .addHeader("cookie", cookieInventory)
                .addHeader("referer", "https://www.mca.gov.in/content/mca/global/en/foportal/fologin.html")
                .addHeader("sec-fetch-dest", "empty")
                .addHeader("sec-fetch-mode", "cors")
                .addHeader("sec-fetch-site", "same-origin")
                .addHeader("x-requested-with", "XMLHttpRequest")
                .addHeader("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36")
                .build();
        try (Response csrfResponse = client.newCall(csrfRequest).execute()) {
            System.out.println("CSRF bootstrap response code: " + csrfResponse.code());
            String csrfBody = csrfResponse.body() == null ? "" : csrfResponse.body().string();
            if (!csrfBody.isBlank()) {
                System.out.println("CSRF bootstrap response body: " + csrfBody.replace("\r", "\\r").replace("\n", "\\n"));
            }
            return this.mergeCookies(cookieInventory, csrfResponse.headers("Set-Cookie"));
        }
        catch (IOException e) {
            System.err.println("Error fetching CSRF bootstrap cookies: " + e.getMessage());
            return cookieInventory;
        }
    }

    private String cookieStringFromHeaders(List<String> cookies) {
        StringBuilder cookieInventory = new StringBuilder();
        for (String header : cookies) {
            String pair = header.split(";")[0];
            cookieInventory.append(pair).append("; ");
        }
        return cookieInventory.toString().trim();
    }

    private String mergeCookies(String oldCookieString, List<String> setCookieHeaders) {
        Map<String, String> cookieMap = new HashMap<>();
        if (oldCookieString != null && !oldCookieString.isEmpty()) {
            String[] pairs = oldCookieString.split(";");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    cookieMap.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
        for (String header : setCookieHeaders) {
            String cookiePart = header.split(";")[0];
            String[] kv = cookiePart.split("=", 2);
            if (kv.length == 2) {
                cookieMap.put(kv[0].trim(), kv[1].trim());
            }
        }
        System.out.println("Bootstrap cookie names: " + cookieMap.keySet());
        StringBuilder updatedCookies = new StringBuilder();
        for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
            if (updatedCookies.length() > 0) {
                updatedCookies.append("; ");
            }
            updatedCookies.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return updatedCookies.toString();
    }

    private String ensureCsrfCookie(String cookieInventory) {
        Map<String, String> cookieMap = this.parseCookieString(cookieInventory);
        if (!cookieMap.containsKey("_csrf") || cookieMap.get("_csrf").isBlank()) {
            String generatedCsrf = UUID.randomUUID().toString();
            cookieMap.put("_csrf", generatedCsrf);
            System.out.println("Generated fallback _csrf cookie: " + generatedCsrf);
        }
        return this.cookieMapToString(cookieMap);
    }

    public String updateCookies(String oldCookieString, Response response) {
        Map<String, String> cookieMap = this.parseCookieString(oldCookieString);
        for (String header : response.headers("Set-Cookie")) {
            String cookiePart = header.split(";")[0];
            String[] kv2 = cookiePart.split("=", 2);
            if (kv2.length != 2) {
                continue;
            }
            String name = kv2[0].trim();
            String value = kv2[1].trim();
            // A blank value (usually paired with Max-Age=0 / a past Expires) is the server deleting
            // the cookie. Storing it verbatim overwrites a live sessionID with "" and then echoes
            // "sessionID=" back on every later call, which MCA reads as "no session" -> 401.
            if (value.isEmpty() || isCookieExpired(header)) {
                if (cookieMap.remove(name) != null) {
                    System.out.println("MCA cleared cookie '" + name + "'; dropping it from the jar");
                }
                continue;
            }
            cookieMap.put(name, value);
        }
        System.out.println("Updated cookie names: " + cookieMap.keySet());
        return this.cookieMapToString(cookieMap);
    }

    // True when the Set-Cookie header carries Max-Age=0 (or negative), i.e. an explicit deletion.
    private boolean isCookieExpired(String setCookieHeader) {
        for (String attribute : setCookieHeader.split(";")) {
            String trimmed = attribute.trim();
            if (trimmed.regionMatches(true, 0, "Max-Age=", 0, "Max-Age=".length())) {
                try {
                    return Long.parseLong(trimmed.substring("Max-Age=".length()).trim()) <= 0L;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    private Map<String, String> parseCookieString(String cookieString) {
        Map<String, String> cookieMap = new HashMap<>();
        if (cookieString != null && !cookieString.isEmpty()) {
            String[] pairs = cookieString.split(";");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    cookieMap.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
        return cookieMap;
    }

    private String cookieMapToString(Map<String, String> cookieMap) {
        StringBuilder updatedCookies = new StringBuilder();
        for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
            if (updatedCookies.length() > 0) {
                updatedCookies.append("; ");
            }
            updatedCookies.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return updatedCookies.toString();
    }
}
