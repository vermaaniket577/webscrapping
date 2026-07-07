package com.mca.automate.controller.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mca.automate.dto.LoginResponse;
import com.mca.automate.dto.MasterDataRequest;
import com.mca.automate.dto.ValidateCaptchaResponse;
import com.mca.automate.service.McaCaptchaService;
import com.mca.automate.service.McaLoginService;
import com.mca.automate.service.McaSearchService;
import com.mca.automate.util.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class MasterDataControllerTest {

    @Mock
    private McaCaptchaService captchaService;

    @Mock
    private McaLoginService loginService;

    @Mock
    private McaSearchService searchService;

    private MasterDataController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new MasterDataController();
        controller.captchaService = captchaService;
        controller.mcaLoginService = loginService;
        controller.mcaSearchService = searchService;
        controller.util = new Util();
    }

    @Test
    void suppliedVerifiedCookieFetchesMasterDataWithoutLoginOrLogout() throws Exception {
        MasterDataRequest request = masterDataRequest();
        ValidateCaptchaResponse captcha = new ValidateCaptchaResponse("Ab12Cd", "pre-ct", "verified-cookie-updated", true);
        when(captchaService.captchaValidatonWrapper("verified-cookie")).thenReturn(captcha);
        when(searchService.search(eq(request), eq(new LoginResponse("verified-cookie-updated", "Authenticated Cookie", true)), eq(captcha)))
                .thenReturn("{\"data\":{\"result\":[{\"cnNmbr\":\"L74909DL2008PLC180850\"}]}}");
        when(searchService.searchMasterData(
                eq("L74909DL2008PLC180850"),
                eq(new LoginResponse("verified-cookie-updated", "Authenticated Cookie", true)),
                eq("{\"data\":{\"result\":[{\"cnNmbr\":\"L74909DL2008PLC180850\"}]}}"),
                eq("cin")))
                .thenReturn("{\"companyData\":{\"CIN\":\"L74909DL2008PLC180850\"}}");

        String response = controller.fetchMasterData(request, "verified-cookie\n");

        assertThat(response).isEqualTo("{\"companyData\":{\"CIN\":\"L74909DL2008PLC180850\"}}");
        verify(loginService, never()).login(any(), any(), any(), any());
        verify(loginService, never()).logout(any());
    }

    @Test
    void loginOtpRequiredReturnsCookieForOtpStep() throws Exception {
        MasterDataRequest request = masterDataRequest();
        when(loginService.login("user@example.com", "secret", "device-1", "login"))
                .thenReturn(new LoginResponse("otp-cookie", "Otp Required", true));

        String response = controller.fetchMasterData(request, null);

        assertThat(response).isEqualTo("otp-cookie");
        verify(captchaService, never()).captchaValidatonWrapper(any());
        verify(searchService, never()).search(any(), any(), any());
    }

    @Test
    void loginSuccessFetchesMasterDataAndLogsOutWhenDone() throws Exception {
        MasterDataRequest request = masterDataRequest();
        LoginResponse login = new LoginResponse("login-cookie", "Login Successful", true);
        ValidateCaptchaResponse captcha = new ValidateCaptchaResponse("Ef34Gh", "pre-ct-2", "captcha-cookie", true);
        when(loginService.login("user@example.com", "secret", "device-1", "login")).thenReturn(login);
        when(captchaService.captchaValidatonWrapper("login-cookie")).thenReturn(captcha);
        when(searchService.search(eq(request), eq(new LoginResponse("captcha-cookie", "Authenticated Cookie", true)), eq(captcha)))
                .thenReturn("search-response");
        when(searchService.searchMasterData(
                eq("L74909DL2008PLC180850"),
                eq(new LoginResponse("captcha-cookie", "Authenticated Cookie", true)),
                eq("search-response"),
                eq("cin")))
                .thenReturn("master-data-response");

        String response = controller.fetchMasterData(request, "");

        assertThat(response).isEqualTo("master-data-response");
        verify(loginService).logout("captcha-cookie");
    }

    @Test
    void suppliedVerifiedCookieFetchesNameSearchWithoutLoginOrLogout() throws Exception {
        MasterDataRequest request = masterDataRequest();
        ValidateCaptchaResponse captcha = new ValidateCaptchaResponse("Gh56Ij", "pre-ct-name", "name-cookie-updated", true);
        when(captchaService.captchaValidatonWrapper("name-cookie")).thenReturn(captcha);
        when(searchService.search(eq(request), eq(new LoginResponse("name-cookie-updated", "Authenticated Cookie", true)), eq(captcha)))
                .thenReturn("{\"data\":{\"result\":[{\"cnNmbr\":\"L74909DL2008PLC180850\",\"cmpnyNm\":\"TEST COMPANY\"}]}}");

        String response = controller.fetchMasterDataWithName(request, "name-cookie\n");

        assertThat(response).isEqualTo("{\"data\":{\"result\":[{\"cnNmbr\":\"L74909DL2008PLC180850\",\"cmpnyNm\":\"TEST COMPANY\"}]}}");
        verify(loginService, never()).login(any(), any(), any(), any());
        verify(loginService, never()).logout(any());
    }

    @Test
    void nameSearchOtpRequiredReturnsCookieForOtpStep() throws Exception {
        MasterDataRequest request = masterDataRequest();
        when(loginService.login("user@example.com", "secret", "device-1", "login"))
                .thenReturn(new LoginResponse("name-otp-cookie", "Otp Required", true));

        String response = controller.fetchMasterDataWithName(request, null);

        assertThat(response).isEqualTo("name-otp-cookie");
        verify(captchaService, never()).captchaValidatonWrapper(any());
        verify(searchService, never()).search(any(), any(), any());
    }

    @Test
    void nameSearchLoginSuccessUsesCaptchaCookieAndLogsOutWhenDone() throws Exception {
        MasterDataRequest request = masterDataRequest();
        LoginResponse login = new LoginResponse("name-login-cookie", "Login Successful", true);
        ValidateCaptchaResponse captcha = new ValidateCaptchaResponse("Kl78Mn", "pre-ct-name-2", "name-captcha-cookie", true);
        when(loginService.login("user@example.com", "secret", "device-1", "login")).thenReturn(login);
        when(captchaService.captchaValidatonWrapper("name-login-cookie")).thenReturn(captcha);
        when(searchService.search(eq(request), eq(new LoginResponse("name-captcha-cookie", "Authenticated Cookie", true)), eq(captcha)))
                .thenReturn("name-search-response");

        String response = controller.fetchMasterDataWithName(request, "");

        assertThat(response).isEqualTo("name-search-response");
        verify(loginService).logout("name-captcha-cookie");
    }

    @Test
    void suppliedVerifiedCookieChecksCompanyNameWithoutLoginOrLogout() throws Exception {
        MasterDataRequest request = masterDataRequest();
        ValidateCaptchaResponse captcha = new ValidateCaptchaResponse("Op12Qr", "pre-ct-check", "check-cookie-updated", true);
        when(captchaService.captchaValidatonWrapper("check-cookie")).thenReturn(captcha);
        when(searchService.checkCompanyName(eq(request), eq(new LoginResponse("check-cookie-updated", "Authenticated Cookie", true)), eq(captcha)))
                .thenReturn("{\"data\":{\"result\":[{\"name\":\"TEST PRIVATE LIMITED\"}]}}");

        String response = controller.checkCompayName(request, "check-cookie\n");

        assertThat(response).isEqualTo("{\"data\":{\"result\":[{\"name\":\"TEST PRIVATE LIMITED\"}]}}");
        verify(loginService, never()).login(any(), any(), any(), any());
        verify(loginService, never()).logout(any());
    }

    @Test
    void checkCompanyNameOtpRequiredReturnsCookieForOtpStep() throws Exception {
        MasterDataRequest request = masterDataRequest();
        when(loginService.login("user@example.com", "secret", "device-1", "login"))
                .thenReturn(new LoginResponse("check-otp-cookie", "Otp Required", true));

        String response = controller.checkCompayName(request, null);

        assertThat(response).isEqualTo("check-otp-cookie");
        verify(captchaService, never()).captchaValidatonWrapper(any());
        verify(searchService, never()).checkCompanyName(any(), any(), any());
    }

    @Test
    void checkCompanyNameLoginSuccessUsesCaptchaCookieAndLogsOutWhenDone() throws Exception {
        MasterDataRequest request = masterDataRequest();
        LoginResponse login = new LoginResponse("check-login-cookie", "Login Successful", true);
        ValidateCaptchaResponse captcha = new ValidateCaptchaResponse("St34Uv", "pre-ct-check-2", "check-captcha-cookie", true);
        when(loginService.login("user@example.com", "secret", "device-1", "login")).thenReturn(login);
        when(captchaService.captchaValidatonWrapper("check-login-cookie")).thenReturn(captcha);
        when(searchService.checkCompanyName(eq(request), eq(new LoginResponse("check-captcha-cookie", "Authenticated Cookie", true)), eq(captcha)))
                .thenReturn("check-company-response");

        String response = controller.checkCompayName(request, "");

        assertThat(response).isEqualTo("check-company-response");
        verify(loginService).logout("check-captcha-cookie");
    }

    private MasterDataRequest masterDataRequest() {
        MasterDataRequest request = new MasterDataRequest();
        request.setCompanyNameOrCIN("L74909DL2008PLC180850");
        request.setUserName("user@example.com");
        request.setPassword("secret");
        request.setDeviceId("device-1");
        return request;
    }
}
