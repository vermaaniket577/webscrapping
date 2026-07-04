package com.mca.automate.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class OtpSessionStore {
    private static final Duration SESSION_TTL = Duration.ofMinutes(20);
    private final Map<String, OtpSession> sessions = new ConcurrentHashMap<>();

    public void remember(String cookie, String sblUserId) {
        String key = this.sessionKey(cookie);
        if (key.isBlank() || sblUserId == null || sblUserId.isBlank()) {
            return;
        }
        // Key by stable MCA cookies so OTP verification can recover sblUserId after cookie updates.
        this.sessions.put(key, new OtpSession(sblUserId.trim(), Instant.now()));
    }

    public String sblUserId(String cookie) {
        String key = this.sessionKey(cookie);
        if (key.isBlank()) {
            return "";
        }
        OtpSession session = this.sessions.get(key);
        if (session == null) {
            return "";
        }
        if (session.createdAt().plus(SESSION_TTL).isBefore(Instant.now())) {
            this.sessions.remove(key);
            return "";
        }
        return session.sblUserId();
    }

    private String sessionKey(String cookie) {
        String uuidHash = this.cookieValue(cookie, "__UUID-HASH");
        if (!uuidHash.isBlank()) {
            return "__UUID-HASH=" + uuidHash;
        }
        // _csrf is less stable than __UUID-HASH, but it is good enough as a fallback for the OTP window.
        String csrf = this.cookieValue(cookie, "_csrf");
        return csrf.isBlank() ? "" : "_csrf=" + csrf;
    }

    private String cookieValue(String cookie, String name) {
        if (cookie == null || cookie.isBlank()) {
            return "";
        }
        for (String part : cookie.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && name.equals(kv[0].trim())) {
                return kv[1].trim();
            }
        }
        return "";
    }

    private record OtpSession(String sblUserId, Instant createdAt) {
    }
}
