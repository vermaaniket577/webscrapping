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

/* JADX INFO: loaded from: OtpController.class */
@RestController
public class OtpController {

    @Autowired
    OtpService otpService;

    @Autowired
    McaLoginService mcaLoginService;

    @Autowired
    OtpSessionStore otpSessionStore;

    @PostMapping({"/verifyotpp"})
    public ResponseEntity<ApiResponse> verifyOtp(@RequestBody VerifyOtpDTO verifyOtpDTO) throws IOException {
        // Recover the login cookie server-side when the client did not send one, so the fragile cookie no longer has to round-trip through JSON.
        if (verifyOtpDTO.getCookie() == null || verifyOtpDTO.getCookie().isBlank()) {
            String recovered = this.otpSessionStore.cookieForClient(verifyOtpDTO.getEmail(), verifyOtpDTO.getDeviceId());
            if (recovered == null || recovered.isBlank()) {
                return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Otp session expired, please login again", null);
            }
            verifyOtpDTO.setCookie(recovered);
        }
        String cookie = this.otpService.verifOtp(verifyOtpDTO);
        if (cookie == null || cookie.isBlank() || "failed".equalsIgnoreCase(cookie)) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Otp Verification Failed", null);
        }
        verifyOtpDTO.setCookie(cookie);
        // MCA OTP validation is only the first step; this follow-up login exchanges it for session cookies.
        String finalCookie = this.mcaLoginService.verifiedOTPLogin(verifyOtpDTO);
        if (finalCookie == null || finalCookie.isBlank() || "false".equalsIgnoreCase(finalCookie)) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Otp Verified but post-OTP login failed", cookie);
        }
        return ResponseUtil.build(HttpStatus.OK, "Otp Verified", finalCookie);
    }
}
