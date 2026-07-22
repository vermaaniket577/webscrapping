package com.mca.automate.controller.authentication;

import com.mca.automate.dto.ApiResponse;
import com.mca.automate.service.CredentialsConfigService;
import com.mca.automate.util.ResponseUtil;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "*")
public class SettingsController {

    @Autowired
    private CredentialsConfigService configService;

    @GetMapping
    public ResponseEntity<ApiResponse> getSettings() {
        Map<String, String> settings = Map.of(
            "username", configService.getUsername(),
            "password", configService.getPassword()
        );
        return ResponseUtil.build(HttpStatus.OK, "Settings retrieved successfully", settings);
    }

    @PostMapping
    public ResponseEntity<ApiResponse> updateSettings(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Username and password cannot be empty", null);
        }
        configService.saveCredentials(username, password);
        return ResponseUtil.build(HttpStatus.OK, "Settings saved successfully", null);
    }
}
