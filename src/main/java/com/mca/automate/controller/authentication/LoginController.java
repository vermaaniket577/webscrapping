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

    @PostMapping({"/roclogin"})
    public ResponseEntity<ApiResponse> loginROCUuser(@RequestBody LoginDTO loginDTO) throws Exception {
        LoginResponse loginResponse = this.mcaLoginService.login(loginDTO.getUserName(), loginDTO.getPassword(), loginDTO.getDeviceId(), "login");
        System.out.println("Login Response is " + loginResponse.status());
        System.out.println("Login Response is " + loginResponse.status() + " cookie is " + loginResponse.cookie());
        if (false == loginResponse.status()) {
            return ResponseUtil.build(HttpStatus.BAD_REQUEST, loginResponse.message(), null);
        }
        return ResponseUtil.build(HttpStatus.OK, loginResponse.message(), null);
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
        if (200 == 200) {
            return ResponseUtil.build(HttpStatus.OK, "Otp Verified Kindly Login", null);
        }
        return ResponseUtil.build(HttpStatus.BAD_REQUEST, "Login Failed!!! ", null);
    }
}
