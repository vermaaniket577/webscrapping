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

    private MasterDataRequest masterDataRequest() {
        MasterDataRequest request = new MasterDataRequest();
        request.setCompanyNameOrCIN("L74909DL2008PLC180850");
        request.setUserName("user@example.com");
        request.setPassword("secret");
        request.setDeviceId("device-1");
        return request;
    }
}
