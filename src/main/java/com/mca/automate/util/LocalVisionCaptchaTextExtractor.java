package com.mca.automate.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LocalVisionCaptchaTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(LocalVisionCaptchaTextExtractor.class);
    private static final String DEFAULT_MODEL = "qwen3-vl:4b";
    private static final String DEFAULT_URL = "http://localhost:11434/api/chat";
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String extractText(String imagePath) throws IOException, InterruptedException {
        long startedAt = System.nanoTime();
        byte[] imageBytes = Files.readAllBytes(Path.of(imagePath));
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String model = System.getProperty("mca.captcha.vision.model", DEFAULT_MODEL);
        String endpoint = System.getProperty("mca.captcha.vision.url", DEFAULT_URL);

        String requestBody = """
                {
                  "model": "%s",
                  "messages": [
                    {
                      "role": "user",
                      "content": "Analyze this captcha image and extract all readable text. Return only the captcha text with no explanation.",
                      "images": ["%s"]
                    }
                  ],
                  "stream": true
                }
                """.formatted(escapeJson(model), base64Image);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<java.io.InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        log.info("Local vision captcha extractor status {} firstResponseMs={}", response.statusCode(), elapsedMs(startedAt));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return "";
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                appendContentToken(content, line);
            }
        }
        String extracted = content.toString().trim();
        log.info("Local vision captcha extractor raw output '{}' totalMs={}", extracted, elapsedMs(startedAt));
        return extracted;
    }

    private void appendContentToken(StringBuilder content, String line) {
        try {
            JsonNode root = mapper.readTree(line);
            String token = root.path("message").path("content").asText("");
            if (!token.isEmpty()) {
                content.append(token);
            }
        }
        catch (Exception ex) {
            log.debug("Skipping unparsable streaming line from local vision extractor: {}", line, ex);
        }
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }
}
