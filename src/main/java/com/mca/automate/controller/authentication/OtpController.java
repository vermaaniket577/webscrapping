package com.mca.automate.controller.authentication;

import com.mca.automate.service.CredentialsConfigService;
import com.mca.automate.dto.ApiResponse;
import com.mca.automate.dto.VerifyOtpDTO;
import com.mca.automate.service.McaLoginService;
import com.mca.automate.service.OtpService;
import com.mca.automate.service.OtpSessionStore;
import com.mca.automate.util.ResponseUtil;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.mca.automate.service.ChromeBrowserService;
import lombok.extern.slf4j.Slf4j;

/* JADX INFO: loaded from: OtpController.class */
@RestController
@Slf4j
public class OtpController {

    @Autowired
    private CredentialsConfigService configService;

    @Autowired
    OtpService otpService;

    @Autowired
    McaLoginService mcaLoginService;

    @Autowired
    OtpSessionStore otpSessionStore;

    @Autowired
    ChromeBrowserService chromeBrowserService;

    private void populateFromSession(VerifyOtpDTO verifyOtpDTO) {
        if (verifyOtpDTO.getDeviceId() != null && !verifyOtpDTO.getDeviceId().isBlank()) {
            OtpSessionStore.ClientSession session = this.otpSessionStore.getSessionByDevice(verifyOtpDTO.getDeviceId());
            if (session != null) {
                if (verifyOtpDTO.getEmail() == null || verifyOtpDTO.getEmail().isBlank()) {
                    verifyOtpDTO.setEmail(session.email());
                }
                if (verifyOtpDTO.getPassword() == null || verifyOtpDTO.getPassword().isBlank()) {
                    verifyOtpDTO.setPassword(session.password());
                }
                if (verifyOtpDTO.getCookie() == null || verifyOtpDTO.getCookie().isBlank()) {
                    verifyOtpDTO.setCookie(session.cookie());
                }
            }
        }
        if (verifyOtpDTO.getEmail() == null || verifyOtpDTO.getEmail().isBlank()) {
            verifyOtpDTO.setEmail(configService.getUsername());
        }
        if (verifyOtpDTO.getPassword() == null || verifyOtpDTO.getPassword().isBlank()) {
            verifyOtpDTO.setPassword(configService.getPassword());
        }
    }

    @PostMapping({ "/verifyotpp" })
    public ResponseEntity<ApiResponse> verifyOtppOriginal(@RequestBody VerifyOtpDTO verifyOtpDTO) throws IOException {
        populateFromSession(verifyOtpDTO);
        String cookie = verifyOtpDTO.getCookie();
        if (cookie == null || cookie.isBlank()) {
            cookie = this.otpSessionStore.cookieForClient(verifyOtpDTO.getEmail(), verifyOtpDTO.getDeviceId());
            verifyOtpDTO.setCookie(cookie);
        }
        cookie = this.otpService.verifOtp(verifyOtpDTO);
        if (cookie == null || cookie.isBlank() || "failed".equalsIgnoreCase(cookie)) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Otp Verification Failed", null);
        }
        verifyOtpDTO.setCookie(cookie);
        // MCA OTP validation is only the first step; this follow-up login exchanges it
        // for session cookies.
        String finalCookie = this.mcaLoginService.verifiedOTPLogin(verifyOtpDTO);
        if ("OTP_ALREADY_VALIDATED".equals(finalCookie)) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "OTP is already validated.", cookie);
        }
        if (finalCookie == null || finalCookie.isBlank() || "false".equalsIgnoreCase(finalCookie)) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Otp Verified but post-OTP login failed", cookie);
        }
        return ResponseUtil.build(HttpStatus.OK, "Otp Verified", finalCookie);
    }
    @PostMapping({ "/confirmOtp" })
    public ResponseEntity<ApiResponse> verifyOtp(@RequestBody VerifyOtpDTO verifyOtpDTO) throws IOException {
        populateFromSession(verifyOtpDTO);
        String cookie = verifyOtpDTO.getCookie();
        if (cookie == null || cookie.isBlank()) {
            cookie = this.otpSessionStore.cookieForClient(verifyOtpDTO.getEmail(), verifyOtpDTO.getDeviceId());
            verifyOtpDTO.setCookie(cookie);
        }
        String mobile = verifyOtpDTO.getMobile();
        cookie = this.otpService.verifOtp(verifyOtpDTO);
        if (cookie == null || cookie.isBlank() || "failed".equalsIgnoreCase(cookie)) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Otp Verification Failed", null);
        }
        verifyOtpDTO.setCookie(cookie);
        // MCA OTP validation is only the first step; this follow-up login exchanges it
        // for session cookies.
        String finalCookie = this.mcaLoginService.verifiedOTPLogin(verifyOtpDTO);
        if ("OTP_ALREADY_VALIDATED".equals(finalCookie)) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "OTP is already validated.", cookie);
        }
        if (finalCookie == null || finalCookie.isBlank() || "false".equalsIgnoreCase(finalCookie)) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Otp Verified but post-OTP login failed", cookie);
        }
        // Inject cookies into browser via CDP and open MCA
        boolean opened = this.chromeBrowserService.openWithCookies(finalCookie, verifyOtpDTO);
        if (!opened) {
            log.warn("CDP cookie injection failed. Returning cookies to frontend as fallback.");
        }
        
        return ResponseUtil.build(HttpStatus.OK, opened ? "Login successful! MCA portal opened."
                : "Login successful! Please use start-chrome.bat for auto-login.", finalCookie);
    }

    @PostMapping({ "/resendotp" })
    public ResponseEntity<ApiResponse> resendOtp(@RequestBody VerifyOtpDTO verifyOtpDTO) throws IOException {
        populateFromSession(verifyOtpDTO);
        String email = verifyOtpDTO.getEmail();

        String cookie = verifyOtpDTO.getCookie();
        if (cookie == null || cookie.isBlank()) {
            cookie = this.otpSessionStore.cookieForClient(email, verifyOtpDTO.getDeviceId());
        }
        if (cookie == null || cookie.isBlank()) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Session expired. Please login again.", null);
        }

        String mobile = verifyOtpDTO.getMobile();

        String result = this.otpService.sendOtp(mobile, email, cookie, "sameOTP", "User_Login");
        if (result == null || result.isBlank() || "failed".equalsIgnoreCase(result)) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Failed to resend OTP", null);
        }

        // Update stored cookie with the fresh one from sendOtp response
        String sblUserId = verifyOtpDTO.getSblUserId();
        if (sblUserId == null || sblUserId.isBlank()) {
            sblUserId = this.otpSessionStore.sblUserId(cookie);
        }
        this.otpSessionStore.remember(result, sblUserId);
        this.otpSessionStore.rememberClient(email, verifyOtpDTO.getDeviceId(), result, sblUserId);
        return ResponseUtil.build(HttpStatus.OK, "OTP resent successfully", null);
    }

    @PostMapping({ "/openmca" })
    public ResponseEntity<ApiResponse> openMca(@RequestBody VerifyOtpDTO verifyOtpDTO) {
        String cookie = verifyOtpDTO.getCookie();
        if (cookie == null || cookie.isBlank()) {
            cookie = this.otpSessionStore.cookieForClient(verifyOtpDTO.getEmail(), verifyOtpDTO.getDeviceId());
        }

        if (cookie == null || cookie.isBlank()) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Valid session cookie is required to open MCA", null);
        }

        boolean opened = this.chromeBrowserService.openWithCookies(cookie, verifyOtpDTO);

        if (opened) {
            return ResponseUtil.build(HttpStatus.OK, "MCA portal opened successfully.", cookie);
        } else {
            return ResponseUtil.build(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to open MCA portal via CDP.", cookie);
        }
    }
}
