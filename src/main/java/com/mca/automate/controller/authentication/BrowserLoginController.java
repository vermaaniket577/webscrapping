package com.mca.automate.controller.authentication;

import com.mca.automate.dto.ApiResponse;
import com.mca.automate.dto.BrowserLoginRequest;
import com.mca.automate.dto.BrowserLoginResponse;
import com.mca.automate.dto.PanUrlLoginRequest;
import com.mca.automate.service.BrowserLoginService;
import com.mca.automate.util.ResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class BrowserLoginController {

    @Autowired
    private BrowserLoginService browserLoginService;

    @PostMapping("/browser-login")
    public ResponseEntity<ApiResponse> browserLogin(@RequestBody BrowserLoginRequest request) {
        log.info("Browser login request received for PAN: {}***",
                request.getPan() != null && request.getPan().length() > 4
                        ? request.getPan().substring(0, 4) : "****");

        // Validate input
        if (request.getPan() == null || request.getPan().isBlank()) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "PAN is required", null);
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Password is required", null);
        }

        // Execute browser login
        BrowserLoginResponse response = browserLoginService.login(
                request.getPan().trim(),
                request.getPassword()
        );

        if (response.success()) {
            return ResponseUtil.build(HttpStatus.OK, response.message(), response.cookies());
        }

        return ResponseUtil.build(HttpStatus.BAD_REQUEST, response.message(), null);
    }

    @PostMapping("/pan-login")
    public ResponseEntity<ApiResponse> panLogin(@RequestBody PanUrlLoginRequest request) {
        log.info("Pan login request received for PAN: {}*** on URL: {}",
                request.getPan() != null && request.getPan().length() > 4
                        ? request.getPan().substring(0, 4) : "****",
                request.getUrl());

        // Validate input
        if (request.getPan() == null || request.getPan().isBlank()) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "PAN is required", null);
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Password is required", null);
        }

        // Execute browser login
        BrowserLoginResponse response;
        if (request.getUrl() == null || request.getUrl().isBlank()) {
            // Fallback to the default configured target URL
            response = browserLoginService.login(
                    request.getPan().trim(),
                    request.getPassword()
            );
        } else {
            response = browserLoginService.loginWithPanAndUrl(
                    request.getUrl(),
                    request.getPan().trim(),
                    request.getPassword()
            );
        }

        if (response.success()) {
            return ResponseUtil.build(HttpStatus.OK, response.message(), response.cookies());
        }

        return ResponseUtil.build(HttpStatus.BAD_REQUEST, response.message(), null);
    }
}
