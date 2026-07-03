package com.mca.automate.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/* JADX INFO: loaded from: JsonUtil.class */
@Component
public class JsonUtil {
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode readTree(String json) {
        try {
            return this.mapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON", e);
        }
    }

    public String extractCin(String json) {
        try {
            JsonNode root = this.mapper.readTree(json);
            return root.path("data").path("result").get(0).path("cnNmbr").asText();
        } catch (Exception e) {
            throw new RuntimeException("Unable to extract CIN", e);
        }
    }
}
