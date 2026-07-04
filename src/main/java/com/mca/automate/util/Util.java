package com.mca.automate.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/* JADX INFO: loaded from: Util.class */
@Component
public class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);
    private static final String HEX = "0123456789ABCDEF";
    private static final SecureRandom random = new SecureRandom();

    public List<MimePart> parseMultipart(byte[] raw, String contentType) throws IOException {
        int bodyEnd;
        log.info("parseMultipart invoked with raw bytes={} and contentType={}", (Object)raw.length, (Object)contentType);
        int bi = contentType.toLowerCase(Locale.ROOT).indexOf("boundary=");
        if (bi < 0) {
            throw new IOException("Multipart without boundary");
        }
        String boundary = contentType.substring(bi + "boundary=".length()).trim();
        if (boundary.startsWith("\"") && boundary.endsWith("\"") && boundary.length() >= 2) {
            boundary = boundary.substring(1, boundary.length() - 1);
        }
        if (boundary.startsWith("--")) {
            boundary = boundary.substring(2);
        }
        log.info("Multipart boundary normalized to '{}'", (Object)boundary);
        byte[] DELIM = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        byte[] END = ("--" + boundary + "--").getBytes(StandardCharsets.ISO_8859_1);
        byte[] CRLF = "\r\n".getBytes(StandardCharsets.ISO_8859_1);
        List<MimePart> parts = new ArrayList<>();
        int start = indexOf(raw, DELIM, 0);
        if (start < 0) {
            log.warn("Starting multipart boundary not found");
            throw new IOException("No starting boundary found");
        }
        log.info("Multipart starting boundary found at byte index {}", (Object)start);
        int pos = start + DELIM.length;
        do {
            if (startsWith(raw, pos, CRLF)) {
                pos += CRLF.length;
            }
            if (startsWith(raw, pos - DELIM.length, END)) {
                break;
            }
            int headerEnd = indexOf(raw, concat(CRLF, CRLF), pos);
            if (headerEnd < 0) {
                log.warn("Multipart header terminator not found at position {}", (Object)pos);
                throw new IOException("Malformed multipart: no header terminator");
            }
            String headerBlock = new String(raw, pos, headerEnd - pos, StandardCharsets.ISO_8859_1);
            headerBlock = headerBlock.replace("\\r\\n", "\r\n").replace("\\n", "\n").replace("\\r", "\r");
            Map<String, String> headers = new LinkedHashMap<>();
            for (String line : headerBlock.split("\r\n")) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                int ci = line.indexOf(58);
                if (ci > 0) {
                    String k = line.substring(0, ci).trim();
                    String v = line.substring(ci + 1).trim();
                    headers.put(k.toLowerCase(Locale.ROOT), v);
                }
            }
            log.info("Parsed multipart headers: {}", (Object)headers);
            int pos2 = headerEnd + (CRLF.length * 2);
            int nextDelim = indexOf(raw, ("\r\n--" + boundary).getBytes(StandardCharsets.ISO_8859_1), pos2);
            int endDelim = indexOf(raw, ("--" + boundary + "--").getBytes(StandardCharsets.ISO_8859_1), pos2);
            if (nextDelim >= 0 && (endDelim < 0 || nextDelim < endDelim)) {
                bodyEnd = nextDelim;
            } else if (endDelim >= 0) {
                bodyEnd = endDelim;
            } else {
                throw new IOException("Malformed multipart: no closing boundary");
            }
            int be = bodyEnd;
            if (be - 2 >= pos2 && raw[be - 2] == 13 && raw[be - 1] == 10) {
                be -= 2;
            }
            byte[] body = Arrays.copyOfRange(raw, pos2, be);
            parts.add(new MimePart(headers, body));
            log.info("Added multipart part with body size {}", (Object)body.length);
            int pos3 = bodyEnd + 2;
            pos = pos3 + boundary.length() + 2;
        } while (!startsWith(raw, pos, "--".getBytes(StandardCharsets.ISO_8859_1)));
        log.info("parseMultipart completed with {} parts", (Object)parts.size());
        return parts;
    }

    private static int indexOf(byte[] source, byte[] pattern, int fromIndex) {
        if (source == null || pattern == null || pattern.length == 0) {
            return -1;
        }
        outer:
        for (int i = Math.max(0, fromIndex); i <= source.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (source[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static boolean startsWith(byte[] a, int offset, byte[] b) {
        if (offset < 0 || offset + b.length > a.length) {
            return false;
        }
        for (int i = 0; i < b.length; i++) {
            if (a[offset + i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    public String safe(String s) {
        return s == null ? "" : s.trim();
    }

    public String stripNewlines(String s) {
        return s.replace("\r", "").replace("\n", "");
    }

    public String extractCIN(String jsonString) throws Exception {
        return this.extractCIN(jsonString, "");
    }

    public String extractCIN(String jsonString, String requestedIdentifier) throws Exception {
        String normalizedRequest = this.normalizeIdentifier(requestedIdentifier);
        if (this.looksLikeCin(normalizedRequest)) {
            // Direct CIN requests should not depend on autosuggest ordering.
            return normalizedRequest;
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonString);
        JsonNode results = root.path("data").path("result");
        if (!results.isArray() || results.isEmpty()) {
            return "";
        }
        if (!normalizedRequest.isBlank()) {
            for (JsonNode result : results) {
                String cnNumber = result.path("cnNmbr").asText();
                if (normalizedRequest.equals(this.normalizeIdentifier(cnNumber))) {
                    // Name searches can return several hits; use the exact requested identifier if it appears.
                    return cnNumber;
                }
            }
        }
        return results.get(0).path("cnNmbr").asText();
    }

    private String normalizeIdentifier(String value) {
        return this.safe(value).replaceAll("\\s+", "").toUpperCase();
    }

    private boolean looksLikeCin(String value) {
        return value != null && value.matches("^[A-Z][0-9]{5}[A-Z]{2}[0-9]{4}[A-Z]{3}[0-9]{6}$");
    }

    public int extractCount(String jsonString) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonString);
        return root.path("data").path("count").asInt();
    }

    public String generateHex(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(HEX.charAt(random.nextInt(HEX.length())));
        }
        return sb.toString();
    }

    public String getCsrf(String cookie) {
        String[] parts = cookie.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("_csrf=")) {
                return trimmed.split("=", 2)[1];
            }
        }
        return null;
    }

    public String stringTOJson(String jsonString) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(jsonString);
        return json;
    }

    public String extractDeviceId(String cookie) {
        String[] parts = cookie.split(";");
        for (String str : parts) {
            String part = str.trim();
            if (part.startsWith("deviceId=")) {
                return part.substring("deviceId=".length());
            }
        }
        return null;
    }

    public String getJsonField(String json, String fieldName) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode outer = mapper.readTree(json);
            JsonNode inner = mapper.readTree(outer.get("resStr").asText());
            return findRecursively(inner, fieldName);
        } catch (Exception e) {
            return null;
        }
    }

    private static String findRecursively(JsonNode node, String fieldName) {
        if (node.has(fieldName)) {
            return node.get(fieldName).asText();
        }
        Iterator it = node.iterator();
        while (it.hasNext()) {
            JsonNode child = (JsonNode) it.next();
            String result = findRecursively(child, fieldName);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
