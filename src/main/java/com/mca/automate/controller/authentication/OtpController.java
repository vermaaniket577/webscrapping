package com.mca.automate.controller.authentication;

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
import lombok.extern.slf4j.Slf4j;
/* JADX INFO: loaded from: OtpController.class */
@RestController
public class OtpController {

    @Autowired
    OtpService otpService;

    @Autowired
    McaLoginService mcaLoginService;

    @Autowired
    OtpSessionStore otpSessionStore;
    
    @Autowired
    ChromeBrowserService chromeBrowserService;

    @PostMapping({"/verifyotpp"})
    public ResponseEntity<ApiResponse> verifyOtp(@RequestBody VerifyOtpDTO verifyOtpDTO) throws IOException {
        String cookie = verifyOtpDTO.getCookie();
        if (cookie == null || cookie.isBlank()) {
            cookie = this.otpSessionStore.getCookieByEmail(verifyOtpDTO.getEmail());
            verifyOtpDTO.setCookie(cookie);
        }
        String mobile = verifyOtpDTO.getMobile();
        if (mobile == null || mobile.isBlank()) {
            mobile = this.otpSessionStore.getMobileByEmail(verifyOtpDTO.getEmail());
            verifyOtpDTO.setMobile(mobile);
        }
        cookie = this.otpService.verifOtp(verifyOtpDTO);
        if (cookie == null || cookie.isBlank() || "failed".equalsIgnoreCase(cookie)) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Otp Verification Failed", null);
        }
        verifyOtpDTO.setCookie(cookie);
        // MCA OTP validation is only the first step; this follow-up login exchanges it for session cookies.
        String finalCookie = this.mcaLoginService.verifiedOTPLogin(verifyOtpDTO);
        if (finalCookie == null || finalCookie.isBlank() || "false".equalsIgnoreCase(finalCookie)) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Otp Verified but post-OTP login failed", cookie);
        }
        // Inject cookies into browser via CDP and open MCA
        boolean opened = this.chromeBrowserService.openWithCookies(finalCookie, verifyOtpDTO);
        if (!opened) {
            log.warn("CDP cookie injection failed. Returning cookies to frontend as fallback.");
        }
        return ResponseUtil.build(HttpStatus.OK, opened ? "Login successful! MCA portal opened." : "Login successful! Please use start-chrome.bat for auto-login.", finalCookie);
    }

    @PostMapping({"/resendotp"})
    public ResponseEntity<ApiResponse> resendOtp(@RequestBody VerifyOtpDTO verifyOtpDTO) throws IOException {
        String email = verifyOtpDTO.getEmail();
        if (email == null || email.isBlank()) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Email is required to resend OTP", null);
        }

        String cookie = verifyOtpDTO.getCookie();
        if (cookie == null || cookie.isBlank()) {
            cookie = this.otpSessionStore.getCookieByEmail(email);
        }
        if (cookie == null || cookie.isBlank()) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Session expired. Please login again.", null);
        }

        String mobile = verifyOtpDTO.getMobile();
        if (mobile == null || mobile.isBlank()) {
            mobile = this.otpSessionStore.getMobileByEmail(email);
        }

        String result = this.otpService.sendOtp(mobile, email, cookie, "sameOTP", "User_Login");
        if (result == null || result.isBlank() || "failed".equalsIgnoreCase(result)) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Failed to resend OTP", null);
        }

        // Update stored cookie with the fresh one from sendOtp response
        this.otpSessionStore.remember(email, result, mobile, "");
        return ResponseUtil.build(HttpStatus.OK, "OTP resent successfully", null);
    }

    @PostMapping({"/openmca"})
    public ResponseEntity<ApiResponse> openMca(@RequestBody VerifyOtpDTO verifyOtpDTO) {
        String cookie = verifyOtpDTO.getCookie();
        if (cookie == null || cookie.isBlank()) {
            cookie = this.otpSessionStore.getCookieByEmail(verifyOtpDTO.getEmail());
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
