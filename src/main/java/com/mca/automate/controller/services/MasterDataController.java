package com.mca.automate.controller.services;

import com.mca.automate.dto.DirectorDataRequest;
import com.mca.automate.dto.LoginResponse;
import com.mca.automate.dto.MasterDataRequest;
import com.mca.automate.dto.ValidateCaptchaResponse;
import com.mca.automate.service.CookieService;
import com.mca.automate.service.McaCaptchaService;
import com.mca.automate.service.McaLoginService;
import com.mca.automate.service.McaSearchService;
import com.mca.automate.util.CryptoUtil;
import com.mca.automate.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/* JADX INFO: loaded from: MasterDataController.class */
@RestController
public class MasterDataController {

    @Autowired
    McaCaptchaService captchaService;

    @Autowired
    McaLoginService mcaLoginService;

    @Autowired
    McaSearchService mcaSearchService;

    @Autowired
    CryptoUtil cryptoUtil;

    @Autowired
    Util util;

    @Autowired
    CookieService cookieService;

    @PostMapping({"/getcompanymasterdata"})
    public String fetchMasterData(@RequestBody MasterDataRequest masterDataRequest, @RequestHeader(value = "Cookie", required = false) String sessionCookie) throws Exception {
        String incomingCookie = this.normalizeIncomingCookie(sessionCookie);
        if (!incomingCookie.isBlank()) {
            // OTP verification returns an authenticated MCA cookie; callers can reuse it here to skip login.
            System.out.println("Using caller supplied MCA session cookie for getcompanymasterdata");
            return this.fetchMasterDataWithAuthenticatedCookie(masterDataRequest, incomingCookie, false);
        }
        LoginResponse loginResponse = this.mcaLoginService.login(masterDataRequest.getUserName(), masterDataRequest.getPassword(), masterDataRequest.getDeviceId(), "login");
        System.out.println("Login Response is " + loginResponse.status() + " cookie is " + loginResponse.cookie());
        if (false == loginResponse.status()) {
            return loginResponse.message();
        }
        if (loginResponse.status() && "Otp Required".equalsIgnoreCase(loginResponse.message())) {
            // Login is intentionally split here: client verifies OTP, then calls back with the verified cookie.
            return loginResponse.cookie();
        }
        return this.fetchMasterDataWithAuthenticatedCookie(masterDataRequest, loginResponse.cookie(), true);
    }

    private String fetchMasterDataWithAuthenticatedCookie(MasterDataRequest masterDataRequest, String cookie, boolean logoutWhenDone) throws Exception {
        LoginResponse loginResponse = new LoginResponse(cookie, "Authenticated Cookie", true);
        // MCA requires a fresh captcha for the search/master-data servlet even after login.
        ValidateCaptchaResponse afterLoginCResponse = this.captchaService.captchaValidatonWrapper(loginResponse.cookie());
        if (!afterLoginCResponse.status()) {
            return "Missing request parameter!!!";
        }
        LoginResponse captchaCookieLoginResponse = new LoginResponse(afterLoginCResponse.cookie(), loginResponse.message(), loginResponse.status());
        System.out.println(afterLoginCResponse.captcha() + "===" + afterLoginCResponse.preCt());
        String fSearchResponseJson = this.mcaSearchService.search(masterDataRequest, captchaCookieLoginResponse, afterLoginCResponse);
        System.out.println("Search Data " + fSearchResponseJson);
        String responseData = this.mcaSearchService.searchMasterData(masterDataRequest.getCompanyNameOrCIN(), captchaCookieLoginResponse, fSearchResponseJson, "cin");
        if (logoutWhenDone) {
            // Do not logout caller-supplied cookies; those are controlled by the OTP/manual client flow.
            this.mcaLoginService.logout(captchaCookieLoginResponse.cookie());
        }
        return responseData;
    }

    private String normalizeIncomingCookie(String cookie) {
        return this.util.stripNewlines(this.util.safe(cookie));
    }

    @PostMapping({"/getcompanymasterdataWithName"})
    public String fetchMasterDataWithName(@RequestBody MasterDataRequest masterDataRequest, @RequestHeader(value = "Cookie", required = false) String sessionCookie) throws Exception {
        String incomingCookie = this.normalizeIncomingCookie(sessionCookie);
        if (!incomingCookie.isBlank()) {
            // OTP verification returns an authenticated MCA cookie; callers can reuse it here to skip login.
            System.out.println("Using caller supplied MCA session cookie for getcompanymasterdataWithName");
            return this.fetchMasterDataWithNameAuthenticatedCookie(masterDataRequest, incomingCookie, false);
        }
        LoginResponse loginResponse = this.mcaLoginService.login(masterDataRequest.getUserName(), masterDataRequest.getPassword(), masterDataRequest.getDeviceId(), "login");
        System.out.println("Login Response is " + loginResponse.status());
        System.out.println("Login Response is " + loginResponse.status() + " cookie is " + loginResponse.cookie());
        if (false == loginResponse.status()) {
            return loginResponse.message();
        }
        if (loginResponse.status() && "Otp Required".equalsIgnoreCase(loginResponse.message())) {
            return loginResponse.cookie();
        }
        return this.fetchMasterDataWithNameAuthenticatedCookie(masterDataRequest, loginResponse.cookie(), true);
    }

    private String fetchMasterDataWithNameAuthenticatedCookie(MasterDataRequest masterDataRequest, String cookie, boolean logoutWhenDone) throws Exception {
        LoginResponse loginResponse = new LoginResponse(cookie, "Authenticated Cookie", true);
        // Name search also needs a captcha token, but should not force a second login after OTP.
        ValidateCaptchaResponse afterLoginCResponse = this.captchaService.captchaValidatonWrapper(loginResponse.cookie());
        if (!afterLoginCResponse.status()) {
            return "Missing request parameter!!!";
        }
        LoginResponse captchaCookieLoginResponse = new LoginResponse(afterLoginCResponse.cookie(), loginResponse.message(), loginResponse.status());
        System.out.println(afterLoginCResponse.captcha() + "===" + afterLoginCResponse.preCt());
        String responseData = this.mcaSearchService.search(masterDataRequest, captchaCookieLoginResponse, afterLoginCResponse);
        if (logoutWhenDone) {
            // Do not logout caller-supplied cookies; those are controlled by the OTP/manual client flow.
            this.mcaLoginService.logout(captchaCookieLoginResponse.cookie());
        }
        return responseData;
    }

    @PostMapping({"/getdirectordata"})
    public String fetchDirData(@RequestBody DirectorDataRequest directorDataRequest) throws Exception {
        System.out.println("inside Din Data+++++++");
        LoginResponse loginResponse = this.mcaLoginService.login(directorDataRequest.getUserName(), directorDataRequest.getPassword(), directorDataRequest.getDeviceId(), "login");
        System.out.println("Login Response is " + loginResponse.status() + " cookie is " + loginResponse.cookie());
        if (false == loginResponse.status()) {
            return this.util.stringTOJson(loginResponse.message());
        }
        if (loginResponse.status() && "Otp Required".equalsIgnoreCase(loginResponse.message())) {
            return this.util.stringTOJson(loginResponse.cookie());
        }
        System.out.println("Login Response is " + loginResponse.status());
        String responseData = this.mcaSearchService.searchMasterData(directorDataRequest.getUserName(), loginResponse, directorDataRequest.getDin(), "din");
        this.mcaLoginService.logout(loginResponse.cookie());
        return responseData;
    }

    @PostMapping({"/checkcompanyname"})
    public String checkCompayName(@RequestBody MasterDataRequest masterDataRequest, @RequestHeader(value = "Cookie", required = false) String sessionCookie) throws Exception {
        String incomingCookie = this.normalizeIncomingCookie(sessionCookie);
        if (!incomingCookie.isBlank()) {
            // Company-name OTP completion sends the verified MCA cookie back on this endpoint.
            System.out.println("Using caller supplied MCA session cookie for checkcompanyname");
            return this.checkCompanyNameWithAuthenticatedCookie(masterDataRequest, incomingCookie, false);
        }
        LoginResponse loginResponse = this.mcaLoginService.login(masterDataRequest.getUserName(), masterDataRequest.getPassword(), masterDataRequest.getDeviceId(), "login");
        System.out.println("Login Response is " + loginResponse.status());
        System.out.println("Login Response is " + loginResponse.status() + " cookie is " + loginResponse.cookie());
        if (false == loginResponse.status()) {
            return loginResponse.message();
        }
        if (loginResponse.status() && "Otp Required".equalsIgnoreCase(loginResponse.message())) {
            return loginResponse.cookie();
        }
        return this.checkCompanyNameWithAuthenticatedCookie(masterDataRequest, loginResponse.cookie(), true);
    }

    private String checkCompanyNameWithAuthenticatedCookie(MasterDataRequest masterDataRequest, String cookie, boolean logoutWhenDone) throws Exception {
        LoginResponse loginResponse = new LoginResponse(cookie, "Authenticated Cookie", true);
        ValidateCaptchaResponse afterLoginCResponse = this.captchaService.captchaValidatonWrapper(loginResponse.cookie());
        if (!afterLoginCResponse.status()) {
            return "Missing request parameter!!!";
        }
        LoginResponse captchaCookieLoginResponse = new LoginResponse(afterLoginCResponse.cookie(), loginResponse.message(), loginResponse.status());
        System.out.println(afterLoginCResponse.captcha() + "===" + afterLoginCResponse.preCt());
        String responseData = this.mcaSearchService.checkCompanyName(masterDataRequest, captchaCookieLoginResponse, afterLoginCResponse);
        if (logoutWhenDone) {
            this.mcaLoginService.logout(captchaCookieLoginResponse.cookie());
        }
        return responseData;
    }

    @GetMapping({"/dinstatus/{din}"})
    public String dinstatus(@PathVariable String din) throws Exception {
        return this.mcaSearchService.checkDinStatus(din);
    }
}
