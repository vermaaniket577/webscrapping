package com.mca.automate.service;

import com.mca.automate.dto.LoginResponse;
import com.mca.automate.dto.MasterDataRequest;
import com.mca.automate.dto.ValidateCaptchaResponse;
import com.mca.automate.util.CryptoUtil;
import com.mca.automate.util.Util;
import java.io.IOException;
import lombok.Generated;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/* JADX INFO: loaded from: McaSearchService.class */
@Service
public class McaSearchService {
    private final HttpClientService http;
    private final CryptoUtil crypto;
    private final Util util;

    @Autowired
    McaCaptchaService captchaService;
    private static final String SEARCH_URL = "https://www.mca.gov.in/bin/mca/mds/commonSearch";
    private static final String CHECK_DIN_STATUS_URL = "https://www.mca.gov.in/bin/mca/foservicedinstatus?data=";
    private static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8");

    @Generated
    public McaSearchService(final HttpClientService http, final CryptoUtil crypto, final Util util) {
        this.http = http;
        this.crypto = crypto;
        this.util = util;
    }

    public String search(MasterDataRequest masterDataRequest, LoginResponse loginResponse, ValidateCaptchaResponse cResponse) throws IOException {
        System.out.println("inside simple search " + cResponse.captcha() + "===" + cResponse.preCt());
        String plain = "module=MDS&searchKeyWord=" + masterDataRequest.getCompanyNameOrCIN() + "&searchType=autosuggest&mdsSearchType=company&userInput=" + cResponse.captcha() + "&pre_CT=" + cResponse.preCt();
        System.out.println("plain body is for simple search " + plain);
        String cookie = this.latestCookie(loginResponse, cResponse);
        String csrf = this.util.getCsrf(cookie);
        if (csrf == null || csrf.isBlank()) {
            System.out.println("MDS commonSearch request blocked because _csrf cookie is missing");
            return "Missing request parameter!!!";
        }
        String encrypted = this.crypto.encrypt(plain);
        String body = "data=" + encrypted + "&csrfToken=" + this.crypto.encrypt(csrf) + "&csrfDecode=false";
        Request req = new Request.Builder().url(SEARCH_URL).post(RequestBody.create(body, FORM)).header("cookie", cookie).header("accept", "application/json, text/javascript, */*; q=0.01").header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8").header("origin", "https://www.mca.gov.in").header("referer", "https://www.mca.gov.in/content/mca/global/en/mca/master-data/MDS.html").header("sec-fetch-dest", "empty").header("sec-fetch-mode", "cors").header("sec-fetch-site", "same-origin").header("x-requested-with", "XMLHttpRequest").header("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").build();
        try (Response resp = this.http.execute(req)) {
            String respBody = resp.body() == null ? "" : resp.body().string();
            System.out.println(" Responsebody is for simple search " + respBody);
            return respBody;
        }
    }

    public String checkCompanyName(MasterDataRequest masterDataRequest, LoginResponse loginResponse, ValidateCaptchaResponse cResponse) throws IOException {
        System.out.println("inside simple search " + cResponse.captcha() + "===" + cResponse.preCt());
        String plain = "module=NameSearch&searchKeyWord=" + this.crypto.encrypt(masterDataRequest.getCompanyNameOrCIN()) + "&searchType=autosuggest&mdsSearchType=searchedName&userInput=" + cResponse.captcha() + "&pre_CT=" + cResponse.preCt();
        System.out.println("plain body is for simple search " + plain);
        String cookie = this.latestCookie(loginResponse, cResponse);
        String csrf = this.util.getCsrf(cookie);
        if (csrf == null || csrf.isBlank()) {
            System.out.println("NameSearch commonSearch request blocked because _csrf cookie is missing");
            return "Missing request parameter!!!";
        }
        String encrypted = this.crypto.encrypt(plain);
        String body = "data=" + encrypted + "&csrfToken=" + this.crypto.encrypt(csrf) + "&csrfDecode=false";
        Request req = new Request.Builder().url(SEARCH_URL).post(RequestBody.create(body, FORM)).header("cookie", cookie).header("accept", "*/*").header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8").header("origin", "https://www.mca.gov.in").header("referer", "https://www.mca.gov.in/content/mca/global/en/mca/fo-llp-services/company-llp-name-search.html").header("sec-fetch-dest", "empty").header("sec-fetch-mode", "cors").header("sec-fetch-site", "same-origin").header("x-requested-with", "XMLHttpRequest").header("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").build();
        try (Response resp = this.http.execute(req)) {
            String respBody = resp.body() == null ? "" : resp.body().string();
            System.out.println(" Responsebody is for simple search " + respBody);
            return respBody;
        }
    }

    public String searchMasterData(String requestedIdentifier, LoginResponse loginResponse, String searchResponse, String requestType) throws Exception {
        ValidateCaptchaResponse cap = this.captchaService.captchaValidatonWrapper(loginResponse.cookie());
        if (!cap.status()) {
            return "Missing request parameter!!!";
        }
        String cookieHeader = this.latestCookie(loginResponse, cap);
        String csrf = this.util.getCsrf(cookieHeader);
        if (csrf == null || csrf.isBlank()) {
            System.out.println("MDS master-data request blocked because _csrf cookie is missing");
            return "Missing request parameter!!!";
        }
        String plainData = "ID=" + searchResponse + "&requestID=din&userInput=" + cap.captcha() + "&pre_CT=" + cap.preCt();
        String data = "data=" + this.crypto.encrypt(plainData) + "&csrfToken=" + this.crypto.encrypt(csrf) + "&csrfDecode=false";
        if ("cin".equalsIgnoreCase(requestType)) {
            // Autosuggest may return multiple companies; prefer the caller's exact CIN when present.
            String cnNumber = this.util.extractCIN(searchResponse, requestedIdentifier);
            String plainData2 = "ID=" + cnNumber + "&requestID=cin&userInput=" + cap.captcha() + "&pre_CT=" + cap.preCt();
            data = "data=" + this.crypto.encrypt(plainData2) + "&csrfToken=" + this.crypto.encrypt(csrf) + "&csrfDecode=false";
        }
        System.out.println("inside main Request Body is " + data);
        System.out.println("inside main cookieHeader " + cookieHeader);
        Request req = new Request.Builder().url("https://www.mca.gov.in/bin/MDSMasterDataServlet").header("accept", "application/json, text/javascript, */*; q=0.01").header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8").header("content-type", "application/x-www-form-urlencoded; charset=UTF-8").header("cookie", cookieHeader).header("origin", "https://www.mca.gov.in").header("referer", "https://www.mca.gov.in/content/mca/global/en/mca/master-data/MDS.html").header("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").header("x-requested-with", "XMLHttpRequest").post(RequestBody.create(data, FORM)).build();
        try (Response resp = this.http.execute(req)) {
            System.out.println("-----------------" + resp.code());
            String respo = resp.body() == null ? "" : resp.body().string();
            System.out.println("Final Search Response body is-----------------" + respo);
            return respo;
        }
    }

    public String checkDinStatus(String din) throws Exception {
        ValidateCaptchaResponse cap = this.captchaService.captchaValidatonWrapper("");
        if (!cap.status()) {
            return "Missing request parameter!!!";
        }
        String data = "DIN=" + din + "&userInput=" + cap.captcha() + "&pre_CT=" + cap.preCt();
        String finalURL = "https://www.mca.gov.in/bin/mca/foservicedinstatus?data=" + this.crypto.encrypt(data);
        Request request = new Request.Builder().url(finalURL).get().addHeader("accept", "*/*").addHeader("accept-language", "en-US,en;q=0.9").addHeader("referer", "https://www.mca.gov.in/content/mca/global/en/application-history.html").addHeader("x-requested-with", "XMLHttpRequest").addHeader("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").addHeader("x-requested-with", "XMLHttpRequest").addHeader("cookie", cap.cookie()).build();
        try (Response resp = this.http.execute(request)) {
            if (resp.code() != 200) {
                return "Internal Error Occured";
            }
            return resp.body() == null ? "" : resp.body().string();
        }
    }

    private String latestCookie(LoginResponse loginResponse, ValidateCaptchaResponse cResponse) {
        if (cResponse != null && cResponse.cookie() != null && !cResponse.cookie().isBlank()) {
            return cResponse.cookie();
        }
        return loginResponse.cookie();
    }
}
