package com.mca.automate.controller.authentication;

import com.mca.automate.dto.ApiResponse;
import com.mca.automate.dto.LoginDTO;
import com.mca.automate.dto.LoginResponse;
import com.mca.automate.dto.NewUserDTO;
import com.mca.automate.dto.NewUserEntity;
import com.mca.automate.dto.VerifyOtpDTO;
import com.mca.automate.service.CookieService;
import com.mca.automate.service.McaCaptchaService;
import com.mca.automate.service.McaLoginService;
import com.mca.automate.service.OtpService;
import com.mca.automate.service.UserRegistration;
import com.mca.automate.util.ResponseUtil;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/* JADX INFO: loaded from: LoginController.class */
@RestController
public class LoginController {

    @Autowired
    McaCaptchaService captchaService;

    @Autowired
    McaLoginService mcaLoginService;

    @Autowired
    CookieService cookieService;

    @Autowired
    UserRegistration registerUser;

    @Autowired
    OtpService otpService;

    @Autowired
    com.mca.automate.util.CookieUtil cookieUtil;

    @GetMapping({"/captcha"})
    public ResponseEntity<ApiResponse> getCaptcha() throws Exception {
        String initialCookie = this.cookieUtil.getResponseCookies();
        com.mca.automate.dto.FetchCaptchaImageResponse response = this.captchaService.getCaptchaImageBase64(initialCookie);
        if (response.status()) {
            return ResponseUtil.build(HttpStatus.OK, "Captcha fetched successfully", response);
        }
        return ResponseUtil.build(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch captcha", null);
    }

    @PostMapping({"/roclogin"})
    public ResponseEntity<ApiResponse> loginROCUuser(@RequestBody LoginDTO loginDTO) throws Exception {
        LoginResponse loginResponse = this.mcaLoginService.login(loginDTO.getUserName(), loginDTO.getPassword(), loginDTO.getDeviceId(), "login", loginDTO.getCaptcha(), loginDTO.getPreCt(), loginDTO.getCookie());
        System.out.println("Login Response is " + loginResponse.status());
        System.out.println("Login Response is " + loginResponse.status() + " cookie is " + loginResponse.cookie());
        if (false == loginResponse.status()) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, loginResponse.message(), null);
        }
        // Open MCA Home Page only when login is fully complete (not when OTP is still pending)
        if (!"Otp Required".equalsIgnoreCase(loginResponse.message())) {
            openMcaHomePage();
        }
        return ResponseUtil.build(HttpStatus.OK, loginResponse.message(), loginResponse);
    }

    @PostMapping({"/rocregister"})
    public ResponseEntity<ApiResponse> registerROCUuser(@RequestBody NewUserDTO newUser) throws Exception {
        String deviceId = UUID.randomUUID().toString().replace("-", "").substring(0, 11);
        NewUserEntity userEntity = new NewUserEntity(newUser.getEmailId(), newUser.getMobileNo(), deviceId, System.currentTimeMillis());
        NewUserEntity savedUser = this.registerUser.createNewUser(userEntity);
        if (savedUser != null && savedUser.getEmailId() != null) {
            return ResponseUtil.build(HttpStatus.OK, "User Registered Successfully", null);
        }
        return ResponseUtil.build(HttpStatus.BAD_REQUEST, "User Registertion Failed", null);
    }

    @PostMapping({"/verifyOtp"})
    public ResponseEntity<ApiResponse> loginROCUuserWithOtp(@RequestBody VerifyOtpDTO verifyOtpDTO) throws Exception {
        String finalCookie = this.mcaLoginService.verifiedOTPLogin(verifyOtpDTO);
        if (!"false".equals(finalCookie)) {
            // Open MCA Home Page in the default browser after successful login
            openMcaHomePage();
            LoginResponse response = new LoginResponse(finalCookie, "Otp Verified Kindly Login", true);
            return ResponseUtil.build(HttpStatus.OK, "Otp Verified & Login Page Opened", response);
        }
        return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Login Failed!!! ", null);
    }

    private void openMcaHomePage() {
        try {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.mca.gov.in/content/mca/global/en/home.html"));
                System.out.println("+++++ MCA Home Page opened in browser: https://www.mca.gov.in/content/mca/global/en/home.html");
            } else {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "https://www.mca.gov.in/content/mca/global/en/home.html"});
                System.out.println("+++++ MCA Home Page opened via cmd: https://www.mca.gov.in/content/mca/global/en/home.html");
            }
        } catch (Exception e) {
            System.out.println("+++++ Failed to open MCA Home Page in browser: " + e.getMessage());
        }
    }
}
