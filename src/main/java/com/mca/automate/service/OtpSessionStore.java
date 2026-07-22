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
    // Keyed by email+deviceId so /verifyotpp can recover the login cookie without the client pasting it.
    private final Map<String, ClientSession> clientSessions = new ConcurrentHashMap<>();
    private final Map<String, ClientSession> deviceSessions = new ConcurrentHashMap<>();

    public void remember(String cookie, String sblUserId) {
        String key = this.sessionKey(cookie);
        if (key.isBlank() || sblUserId == null || sblUserId.isBlank()) {
            return;
        }
        // Key by stable MCA cookies so OTP verification can recover sblUserId after cookie updates.
        this.sessions.put(key, new OtpSession(sblUserId.trim(), Instant.now()));
    }

    public void rememberClient(String email, String deviceId, String cookie, String sblUserId) {
        String existingPassword = "";
        ClientSession existing = getSessionByDevice(deviceId);
        if (existing != null) {
            existingPassword = existing.password();
        }
        rememberClient(email, deviceId, cookie, sblUserId, existingPassword);
    }

    public void rememberClient(String email, String deviceId, String cookie, String sblUserId, String password) {
        String key = this.clientKey(email, deviceId);
        if (key.isBlank() || cookie == null || cookie.isBlank()) {
            return;
        }
        ClientSession session = new ClientSession(email, password == null ? "" : password, cookie, sblUserId == null ? "" : sblUserId.trim(), Instant.now());
        this.clientSessions.put(key, session);
        if (deviceId != null && !deviceId.isBlank()) {
            this.deviceSessions.put(deviceId.trim(), session);
        }
    }

    public ClientSession getSessionByDevice(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return null;
        }
        ClientSession session = this.deviceSessions.get(deviceId.trim());
        if (session == null) {
            return null;
        }
        if (session.createdAt().plus(SESSION_TTL).isBefore(Instant.now())) {
            this.deviceSessions.remove(deviceId.trim());
            return null;
        }
        return session;
    }

    public String cookieForClient(String email, String deviceId) {
        String key = this.clientKey(email, deviceId);
        if (key.isBlank()) {
            return "";
        }
        ClientSession session = this.clientSessions.get(key);
        if (session == null) {
            return "";
        }
        if (session.createdAt().plus(SESSION_TTL).isBefore(Instant.now())) {
            this.clientSessions.remove(key);
            return "";
        }
        return session.cookie();
    }

    private String clientKey(String email, String deviceId) {
        if (email == null || email.isBlank() || deviceId == null || deviceId.isBlank()) {
            return "";
        }
        return email.trim().toLowerCase() + "|" + deviceId.trim();
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

    public record ClientSession(String email, String password, String cookie, String sblUserId, Instant createdAt) {
    }
}
