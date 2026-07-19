package com.mca.automate.controller.authentication;

import com.mca.automate.dto.ApiResponse;
import com.mca.automate.dto.VerifyOtpDTO;
import com.mca.automate.service.ChromeBrowserService;
import com.mca.automate.service.McaLoginService;
import com.mca.automate.service.OtpService;
import com.mca.automate.util.ResponseUtil;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/* JADX INFO: loaded from: OtpController.class */
@RestController
public class OtpController {

    @Autowired
    OtpService otpService;

    @Autowired
    McaLoginService mcaLoginService;

    @Autowired
    com.mca.automate.service.OtpSessionStore otpSessionStore;

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
        // Open MCA Home Page in Chrome with session cookies injected via CDP
        boolean cdpSuccess = this.chromeBrowserService.openWithCookies(finalCookie);
        if (!cdpSuccess) {
            return ResponseUtil.build(HttpStatus.INTERNAL_SERVER_ERROR, "OTP Verified, but failed to connect to Chrome. Make sure Chrome was started with --remote-debugging-port=9222", finalCookie);
        }
        return ResponseUtil.build(HttpStatus.OK, "Otp Verified & Login Page Opened", finalCookie);
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
}
