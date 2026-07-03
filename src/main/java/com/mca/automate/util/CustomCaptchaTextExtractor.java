package com.mca.automate.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomCaptchaTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(CustomCaptchaTextExtractor.class);
    private static final Pattern CAPTCHA_PATTERN = Pattern.compile("[A-Za-z0-9]{4,10}");
    private static final Pattern QUOTED_PROBABILITY_PATTERN = Pattern.compile("[\"']?([A-Za-z0-9]{4,10})[\"']?\\s*[:=\\-]\\s*[\"']?([0-9]+(?:\\.[0-9]+)?%?)[\"']?");
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("(?is)```(?:json)?\\s*(.*?)\\s*```");
    private static final Set<String> IGNORED_TOKENS = Set.of(
            "captcha", "probability", "confidence", "guess", "guesses", "text", "value",
            "answer", "possible", "json", "return", "model", "parts", "content"
    );
    private static final String DEFAULT_API_KEY = "";
    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    public Map<String, Double> extractText(String imagePath) throws IOException, InterruptedException {
        String apiKey = firstNonBlank(System.getenv("GEMINI_API_KEY"), DEFAULT_API_KEY);
        String model = firstNonBlank(System.getenv("GEMINI_MODEL"), DEFAULT_MODEL);
        byte[] imageBytes = Files.readAllBytes(Path.of(imagePath));
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String mimeType = getMimeType(imagePath);

        String requestBody = """
        {
          "contents": [{
            "parts": [
              {
                "text": "Read this captcha image. Return only a JSON object where each key is one possible captcha text and each value is a confidence probability from 0 to 1. Do not include spaces in captcha text. Example: {\\\"aB12xY\\\": 0.9}"
              },
              {
                "inline_data": {
                  "mime_type": "%s",
                  "data": "%s"
                }
              }
            ]
          }]
        }
        """.formatted(mimeType, base64Image);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("Custom captcha extractor status {}", response.statusCode());
        log.info("Custom captcha extractor response body {}", response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return Map.of();
        }
        Map<String, Double> guesses = parseGuesses(response.body());
        if (guesses.isEmpty()) {
            log.warn("Custom captcha extractor produced no parsed guesses");
        } else {
            guesses.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(entry -> log.info("Custom captcha prediction '{}' probability {}", entry.getKey(), entry.getValue()));
        }
        return guesses;
    }

    private Map<String, Double> parseGuesses(String responseBody) {
        LinkedHashMap<String, Double> guesses = new LinkedHashMap<>();
        try {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            for (JsonNode candidate : candidates) {
                JsonNode parts = candidate.path("content").path("parts");
                for (JsonNode part : parts) {
                    String text = part.path("text").asText("");
                    addGuessesFromModelText(guesses, text);
                }
            }
            if (guesses.isEmpty()) {
                addGuessesFromJsonNode(guesses, root);
            }
        }
        catch (Exception ex) {
            log.warn("Could not parse custom captcha extractor outer response, trying raw fallback", ex);
            addGuessesFromModelText(guesses, responseBody);
        }
        return guesses;
    }

    private void addGuessesFromModelText(LinkedHashMap<String, Double> guesses, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String cleanedText = cleanModelText(text);
        for (String jsonCandidate : jsonCandidates(cleanedText)) {
            if (tryAddGuessesFromJson(guesses, jsonCandidate)) {
                return;
            }
        }
        addGuessesFromKeyValueText(guesses, cleanedText);
        addGuessesFromPlainTokens(guesses, cleanedText);
    }

    private boolean tryAddGuessesFromJson(LinkedHashMap<String, Double> guesses, String jsonText) {
        try {
            JsonNode guessJson = mapper.readTree(jsonText);
            int before = guesses.size();
            addGuessesFromJsonNode(guesses, guessJson);
            return guesses.size() > before;
        }
        catch (Exception ignored) {
            return false;
        }
    }

    private void addGuessesFromJsonNode(LinkedHashMap<String, Double> guesses, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (looksLikeCaptcha(key)) {
                    addGuess(guesses, key, probabilityFrom(value, 1.0));
                } else if (value.isTextual() && looksLikeCaptcha(value.asText())) {
                    addGuess(guesses, value.asText(), probabilityFrom(node.path("probability"), 1.0));
                } else {
                    addGuessesFromJsonNode(guesses, value);
                }
            });
            String guess = firstText(node, "guess", "captcha", "text", "answer", "value");
            if (looksLikeCaptcha(guess)) {
                addGuess(guesses, guess, probabilityFrom(firstNode(node, "probability", "confidence", "score"), 1.0));
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                addGuessesFromJsonNode(guesses, child);
            }
            return;
        }
        if (node.isTextual() && looksLikeCaptcha(node.asText())) {
            addGuess(guesses, node.asText(), 1.0);
        }
    }

    private void addGuessesFromKeyValueText(LinkedHashMap<String, Double> guesses, String text) {
        Matcher matcher = QUOTED_PROBABILITY_PATTERN.matcher(text);
        while (matcher.find()) {
            addGuess(guesses, matcher.group(1), parseProbability(matcher.group(2), 1.0));
        }
    }

    private void addGuessesFromPlainTokens(LinkedHashMap<String, Double> guesses, String text) {
        Matcher matcher = CAPTCHA_PATTERN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            if (looksLikeCaptcha(token)) {
                addGuess(guesses, token, 1.0);
            }
        }
    }

    private List<String> jsonCandidates(String text) {
        List<String> candidates = new ArrayList<>();
        addIfNotBlank(candidates, text);
        addIfNotBlank(candidates, extractBetween(text, '{', '}'));
        addIfNotBlank(candidates, extractBetween(text, '[', ']'));
        addIfNotBlank(candidates, repairJson(text));
        addIfNotBlank(candidates, repairJson(extractBetween(text, '{', '}')));
        addIfNotBlank(candidates, repairJson(extractBetween(text, '[', ']')));
        return candidates.stream().distinct().toList();
    }

    private String cleanModelText(String text) {
        String cleaned = text.trim()
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace("\\/", "/");
        Matcher fenceMatcher = CODE_FENCE_PATTERN.matcher(cleaned);
        if (fenceMatcher.find()) {
            cleaned = fenceMatcher.group(1).trim();
        }
        cleaned = stripOuterJsonString(cleaned);
        return cleaned.trim();
    }

    private String stripOuterJsonString(String text) {
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            try {
                return mapper.readValue(text, String.class);
            }
            catch (Exception ignored) {
                return text.substring(1, text.length() - 1).replace("\\\"", "\"");
            }
        }
        return text;
    }

    private String extractBetween(String text, char startChar, char endChar) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int start = text.indexOf(startChar);
        int end = text.lastIndexOf(endChar);
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return "";
    }

    private String repairJson(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String repaired = text
                .replaceAll("(?m)//.*$", "")
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll(",\\s*([}\\]])", "$1")
                .trim();
        if (repaired.startsWith("[") && repaired.endsWith("]")) {
            return repaired;
        }
        if (!repaired.startsWith("{") && repaired.contains(":")) {
            repaired = "{" + repaired;
        }
        if (repaired.startsWith("{") && !repaired.endsWith("}")) {
            repaired = repaired + "}";
        }
        return repaired;
    }

    private JsonNode firstNode(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (!value.isMissingNode() && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private String firstText(JsonNode node, String... names) {
        JsonNode value = firstNode(node, names);
        return value == null ? "" : value.asText("");
    }

    private double probabilityFrom(JsonNode node, double fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        if (node.isNumber()) {
            return normalizeProbability(node.asDouble(fallback));
        }
        return parseProbability(node.asText(""), fallback);
    }

    private double parseProbability(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            String cleaned = value.trim().replace("%", "");
            double parsed = Double.parseDouble(cleaned);
            if (value.contains("%") || parsed > 1.0) {
                parsed = parsed / 100.0;
            }
            return normalizeProbability(parsed);
        }
        catch (Exception ignored) {
            return fallback;
        }
    }

    private double normalizeProbability(double probability) {
        if (Double.isNaN(probability) || Double.isInfinite(probability)) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, probability));
    }

    private void addGuess(LinkedHashMap<String, Double> guesses, String guess, double probability) {
        String normalized = guess == null ? "" : guess.replaceAll("[^A-Za-z0-9]", "");
        if (looksLikeCaptcha(normalized)) {
            guesses.merge(normalized, normalizeProbability(probability), Math::max);
        }
    }

    private boolean looksLikeCaptcha(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.replaceAll("[^A-Za-z0-9]", "");
        if (!CAPTCHA_PATTERN.matcher(normalized).matches()) {
            return false;
        }
        return !IGNORED_TOKENS.contains(normalized.toLowerCase());
    }

    private void addIfNotBlank(List<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }

    private String getMimeType(String imagePath) {
        String lower = imagePath.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }
}
