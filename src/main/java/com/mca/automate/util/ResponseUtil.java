package com.mca.automate.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mca.automate.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/* JADX INFO: loaded from: ResponseUtil.class */
public class ResponseUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static ResponseEntity<ApiResponse> build(HttpStatus status, String message, Object data) {
        String strWriteValueAsString;
        if (data == null) {
            strWriteValueAsString = null;
        } else {
            try {
                strWriteValueAsString = mapper.writeValueAsString(data);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(500, "Serialization Error", null));
            }
        }
        String jsonData = strWriteValueAsString;
        return ResponseEntity.status(status).body(new ApiResponse(status.value(), message, jsonData));
    }
}
