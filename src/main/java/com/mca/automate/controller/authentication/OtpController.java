package com.mca.automate.controller.authentication;

import com.mca.automate.dto.ApiResponse;
import com.mca.automate.dto.VerifyOtpDTO;
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

    @PostMapping({"/verifyotpp"})
    public ResponseEntity<ApiResponse> verifyOtp(@RequestBody VerifyOtpDTO verifyOtpDTO) throws IOException {
        String cookie = this.otpService.verifOtp(verifyOtpDTO);
        if (cookie == null || cookie.isBlank() || "failed".equalsIgnoreCase(cookie)) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Otp Verification Failed", null);
        }
        verifyOtpDTO.setCookie(cookie);
        String finalCookie = this.mcaLoginService.verifiedOTPLogin(verifyOtpDTO);
        if (finalCookie == null || finalCookie.isBlank() || "false".equalsIgnoreCase(finalCookie)) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Otp Verified but post-OTP login failed", cookie);
        }
        return ResponseUtil.build(HttpStatus.OK, "Otp Verified", finalCookie);
    }
}
