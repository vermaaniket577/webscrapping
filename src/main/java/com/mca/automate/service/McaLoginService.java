package com.mca.automate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mca.automate.dto.LoginResponse;
import com.mca.automate.dto.ValidateCaptchaResponse;
import com.mca.automate.dto.VerifyOtpDTO;
import com.mca.automate.repository.CookieRepository;
import com.mca.automate.util.CookieUtil;
import com.mca.automate.util.CryptoUtil;
import com.mca.automate.util.CustomSHA;
import com.mca.automate.util.Util;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.List;
import lombok.Generated;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/* JADX INFO: loaded from: McaLoginService.class */
@Service
public class McaLoginService {
    private final HttpClientService http;
    private final CryptoUtil crypto;

    @Autowired
    CookieService cookieService;

    @Autowired
    CustomSHA customSHA;

    @Autowired
    McaCaptchaService mcaCaptchaService;

    @Autowired
    Util util;

    @Autowired
    CookieUtil cookieUtil;

    @Autowired
    CookieRepository cookieRepository;

    @Autowired
    UserRegistration userRegistration;

    @Autowired
    OtpService otpService;

    @Autowired
    OtpSessionStore otpSessionStore;
    private static final String LOGIN_URL = "https://www.mca.gov.in/content/mca/global/en/foportal/fologin/jcr:content/root/responsivegrid/fologin.FOUserLoginAPI.json";
    private static final String LOGOUT_URL = "https://www.mca.gov.in/bin/mca/logout";

    @Generated
    private static final Logger log = LoggerFactory.getLogger(McaLoginService.class);
    private static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8");
    private static final int LOGIN_LOCK_COUNT = 64;
    private static final Object[] LOGIN_LOCKS = createLoginLocks();

    @Generated
    public McaLoginService(final HttpClientService http, final CryptoUtil crypto) {
        this.http = http;
        this.crypto = crypto;
    }

    public LoginResponse login(String username, String password, String deviceId, String requestType) {
        synchronized (this.loginLockFor(username)) {
            return this.doLogin(username, password, deviceId, requestType);
        }
    }

    private LoginResponse doLogin(String username, String password, String deviceId, String requestType) {
        try {
            String newCookie = this.cookieUtil.getResponseCookies();
            newCookie = this.withDeviceCookie(newCookie, deviceId);
            ValidateCaptchaResponse cap = this.mcaCaptchaService.captchaValidatonWrapper(newCookie);
            if (!cap.status()) {
                System.out.println("Captcha Validation Failed......");
                return new LoginResponse("", "Login Failed please connect to Administrator", false);
            }
            String loginCookie = cap.cookie();
            String loginPayLoad = "userName=" + username.toUpperCase() + "&password=" + this.customSHA.calcSHA(password) + "&deviceId=" + deviceId + "&userInput=" + this.util.stripNewlines(this.util.safe(cap.captcha())) + "&pre_CT=" + this.util.stripNewlines(this.util.safe(cap.preCt())) + "&paramData=null";
            String csrf = this.util.stripNewlines(this.util.safe(this.util.getCsrf(loginCookie)));
            if (csrf.isBlank()) {
                System.out.println("+++++++++ Login blocked because _csrf cookie is missing from cookie jar");
                return new LoginResponse(loginCookie, "Login failed because _csrf cookie is missing", false);
            }
            String encryptedData = this.crypto.encrypt(loginPayLoad) + "&csrfToken=" + this.crypto.encrypt(csrf) + "&csrfDecode=false";
            System.out.println("+++++++++ Login plain request Data is " + loginPayLoad);
            System.out.println("+++++++++ Login Request cookie is " + loginCookie);
            try (Response loginResponse = loginRequestExecuter(LOGIN_URL, encryptedData, loginCookie)) {
                String newCookie2 = this.cookieUtil.updateCookies(loginCookie, loginResponse);
                String loginResponseBody = loginResponse.body() == null ? "" : loginResponse.body().string();
                System.out.println("+++++++++ Login response  code is " + loginResponse.code());
                if (loginResponse.code() != 200) {
                    System.out.println("+++++++++ Login failed before encrypted response. Body excerpt is " + this.htmlErrorExcerpt(loginResponseBody));
                    return new LoginResponse(newCookie2, "Login failed with HTTP " + loginResponse.code(), false);
                }
                System.out.println("+++++++++ Login response  Body is is " + this.crypto.decrypt(loginResponseBody));
                String sessionId = "";
                String sessionMd5 = "";
                System.out.println("+++++++++++++++++++ Login Response cookies is to check md5 " + loginResponse.headers("Set-Cookie"));
                String rawResponse = this.crypto.decrypt(loginResponseBody);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(rawResponse);
                String resCode = rootNode.get("resCode").asText();
                if ("208".equalsIgnoreCase(resCode)) {
                    System.out.println("++++++ Inside 208 Force Login ");
                    String forceLoginPayLoad = "previousSession=killActiveSession&userName=" + username.toUpperCase() + "&password=" + this.customSHA.calcSHA(password) + "&deviceId=" + deviceId + "&userInput=" + this.util.stripNewlines(this.util.safe(cap.captcha())) + "&pre_CT=" + this.util.stripNewlines(this.util.safe(cap.preCt())) + "&paramData=null";
                    String forceLoginCsrf = this.util.stripNewlines(this.util.safe(this.util.getCsrf(newCookie2)));
                    if (forceLoginCsrf.isBlank()) {
                        return new LoginResponse(newCookie2, "Force login failed because _csrf cookie is missing", false);
                    }
                    String forceLoginPayLoadEncrypted = this.crypto.encrypt(forceLoginPayLoad) + "&csrfToken=" + this.crypto.encrypt(forceLoginCsrf) + "&csrfDecode=false";
                    try (Response forceLoginResponse = loginRequestExecuter(LOGIN_URL, forceLoginPayLoadEncrypted, newCookie2)) {
                        String forceLoginBody = forceLoginResponse.body() == null ? "" : forceLoginResponse.body().string();
                        String forceLoginCookie = this.cookieUtil.updateCookies(newCookie2, forceLoginResponse);
                        System.out.println("++++++ Inside 208 response code is " + forceLoginResponse.code());
                        if (forceLoginResponse.code() != 200 || forceLoginBody.isBlank()) {
                            return new LoginResponse(forceLoginCookie, "Force login failed with HTTP " + forceLoginResponse.code(), false);
                        }
                        // HTTP 200 only means MCA accepted the POST; the verdict is the encrypted resCode.
                        String forceLoginRaw = this.crypto.decrypt(forceLoginBody);
                        System.out.println("++++++ Inside 208 decrypted response body is " + forceLoginRaw);
                        String forceLoginResCode = mapper.readTree(forceLoginRaw).path("resCode").asText();
                        if ("206".equalsIgnoreCase(forceLoginResCode)) {
                            String forceMobile = this.util.getJsonField(forceLoginRaw, "mobile");
                            String forceEmail = this.util.getJsonField(forceLoginRaw, "email");
                            String forceSblUserId = this.util.getJsonField(forceLoginRaw, "sblUserId");
                            String forceOtpCookie = this.otpService.sendOtp(forceMobile, forceEmail, forceLoginCookie, "sameOTP", "User_Login");
                            this.otpSessionStore.remember(forceOtpCookie, forceSblUserId);
                            this.otpSessionStore.rememberClient(username, deviceId, forceOtpCookie, forceSblUserId, password);
                            return new LoginResponse(forceOtpCookie, "Otp Required", true);
                        }
                        if (!"200".equalsIgnoreCase(forceLoginResCode)) {
                            return new LoginResponse(forceLoginCookie, this.mcaLoginErrorMessage(forceLoginRaw), false);
                        }
                        // Same guard as the direct-login path: no session cookies means no usable session,
                        // and reporting success here is what leaves callers with a silent 401 downstream.
                        String forceSessionId = "";
                        String forceSessionMd5 = "";
                        for (String h : forceLoginResponse.headers("Set-Cookie")) {
                            for (HttpCookie c : HttpCookie.parse(h)) {
                                if (c.getName().equals("sessionID")) {
                                    forceSessionId = c.getValue();
                                }
                                if (c.getName().equals("session-token-md5")) {
                                    forceSessionMd5 = c.getValue();
                                }
                            }
                        }
                        System.out.println("++++++ Force login Session Id is " + forceSessionId + " +++++ Session md5 is " + forceSessionMd5);
                        if (forceSessionId.isEmpty() || forceSessionMd5.isEmpty()) {
                            return new LoginResponse(forceLoginCookie, "Login Failed please connect to Administrator", false);
                        }
                        return new LoginResponse(forceLoginCookie, "Login Successful", true);
                    }
                }
                if ("206".equalsIgnoreCase(resCode)) {
                    System.out.println("++++++ Inside 206");
                    String mobile = this.util.getJsonField(rawResponse, "mobile");
                    String email = this.util.getJsonField(rawResponse, "email");
                    String sblUserId = this.util.getJsonField(rawResponse, "sblUserId");
                    String newCookie3 = this.otpService.sendOtp(mobile, email, newCookie2, "sameOTP", "User_Login");
                    String anyError = this.util.getJsonField(rawResponse, "errorMessage3");
                    if ("" != anyError) {
                        newCookie3 = this.otpService.sendOtp(mobile, email, newCookie3, "onlyEmailOTP", "New_Device_Notify");
                    }
                    this.otpSessionStore.remember(newCookie3, sblUserId);
                    // Also stash the cookie by email+deviceId so /verifyotpp can recover it without the client pasting it back.
                    this.otpSessionStore.rememberClient(username, deviceId, newCookie3, sblUserId, password);
                    return new LoginResponse(newCookie3, "Otp Required", true);
                }
                if (!"200".equalsIgnoreCase(resCode)) {
                    return new LoginResponse(newCookie2, this.mcaLoginErrorMessage(rawResponse), false);
                }
                for (String h : loginResponse.headers("Set-Cookie")) {
                    for (HttpCookie c : HttpCookie.parse(h)) {
                        if (c.getName().equals("sessionID")) {
                            sessionId = c.getValue();
                        }
                        if (c.getName().equals("session-token-md5")) {
                            sessionMd5 = c.getValue();
                        }
                    }
                }
                System.out.println("+++++++++ Session Id is" + sessionId + " +++++ Session md5 is " + sessionMd5);
                if ("".equalsIgnoreCase(sessionId) || "".equalsIgnoreCase(sessionMd5)) {
                    return new LoginResponse("", "Login Failed please connect to Administrator", false);
                }
                return new LoginResponse(this.cookieUtil.updateCookies(newCookie2, loginResponse), "Login Successful", true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new LoginResponse("", "Login Failed please connect to Administrator", false);
        }
    }

    private String mcaLoginErrorMessage(String rawResponse) {
        String errorMessage1 = this.util.getJsonField(rawResponse, "errorMessage1");
        if (errorMessage1 != null && !errorMessage1.isBlank()) {
            return errorMessage1;
        }
        String message = this.util.getJsonField(rawResponse, "message");
        if (message != null && !message.isBlank()) {
            return message;
        }
        return "Login Failed please connect to Administrator";
    }

    private static Object[] createLoginLocks() {
        Object[] locks = new Object[LOGIN_LOCK_COUNT];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
        return locks;
    }

    private Object loginLockFor(String username) {
        String key = username == null ? "" : username.trim().toLowerCase();
        return LOGIN_LOCKS[Math.floorMod(key.hashCode(), LOGIN_LOCKS.length)];
    }

    public void logout(String cookie) throws IOException {
        Request req = new Request.Builder().url(LOGOUT_URL).post(RequestBody.create("", (MediaType) null)).header("accept", "*/*").header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8").header("origin", "https://www.mca.gov.in").header("referer", "https://www.mca.gov.in/content/mca/global/en/foportal/fologin.html").header("sec-fetch-dest", "empty").header("sec-fetch-mode", "cors").header("sec-fetch-site", "same-origin").header("x-requested-with", "XMLHttpRequest").header("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").header("Cookie", cookie).build();
        try (Response resp = this.http.execute(req)) {
            System.out.println("+++++ Logout response code= " + resp.code() + "++++ Logout response body is " + (resp.body() == null ? "" : resp.body().string()));
        }
    }

    public Response loginRequestExecuter(String URL, String encrypted, String cookie) {
        RequestBody body = RequestBody.create("data=" + encrypted, FORM);
        Request req = new Request.Builder().url(URL).post(body).header("accept", "text/plain, */*; q=0.01").header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8").header("content-type", "application/x-www-form-urlencoded; charset=UTF-8").header("origin", "https://www.mca.gov.in").header("referer", "https://www.mca.gov.in/content/mca/global/en/foportal/fologin.html").header("sec-ch-ua", "\"Google Chrome\";v=\"149\", \"Chromium\";v=\"149\", \"Not)A;Brand\";v=\"24\"").header("sec-ch-ua-mobile", "?0").header("sec-fetch-dest", "empty").header("sec-fetch-mode", "cors").header("sec-fetch-site", "same-origin").header("csrf-token", "undefined").header("x-requested-with", "XMLHttpRequest").header("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").header("Cookie", cookie).build();
        Response resp = this.http.execute(req);
        return resp;
    }

    private String withDeviceCookie(String cookie, String deviceId) {
        String safeDeviceId = this.util.stripNewlines(this.util.safe(deviceId));
        if (safeDeviceId.isBlank()) {
            return cookie;
        }
        String safeCookie = this.util.stripNewlines(this.util.safe(cookie));
        StringBuilder merged = new StringBuilder();
        boolean deviceCookieWritten = false;
        for (String part : safeCookie.split(";")) {
            String trimmed = part.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            if (merged.length() > 0) {
                merged.append("; ");
            }
            if (trimmed.startsWith("deviceId=")) {
                merged.append("deviceId=").append(safeDeviceId);
                deviceCookieWritten = true;
            } else {
                merged.append(trimmed);
            }
        }
        if (!deviceCookieWritten) {
            if (merged.length() > 0) {
                merged.append("; ");
            }
            merged.append("deviceId=").append(safeDeviceId);
        }
        return merged.toString();
    }

    private String printable(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r", "\\r").replace("\n", "\\n");
    }

    private String htmlErrorExcerpt(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String printable = this.printable(value);
        int apologiesIndex = printable.indexOf("Our apologies");
        if (apologiesIndex >= 0) {
            return printable.substring(apologiesIndex, Math.min(printable.length(), apologiesIndex + 260));
        }
        int titleIndex = printable.indexOf("<title>");
        if (titleIndex >= 0) {
            return printable.substring(titleIndex, Math.min(printable.length(), titleIndex + 260));
        }
        return printable.substring(0, Math.min(printable.length(), 260));
    }

    public String verifiedOTPLogin(VerifyOtpDTO verifyOtpDTO) {
        String sblUserId = this.util.stripNewlines(this.util.safe(verifyOtpDTO.getSblUserId()));
        if (sblUserId.isBlank()) {
            // Initial login stores sblUserId by cookie so the OTP API does not need clients to pass it around.
            sblUserId = this.otpSessionStore.sblUserId(verifyOtpDTO.getCookie());
        }
        if (sblUserId.isBlank()) {
            System.out.println("+++++ verify otp login blocked because sblUserId is missing from OTP session");
            return "false";
        }
        String loginPayLoad = "userName=" + verifyOtpDTO.getEmail().toUpperCase() + "&password=" + this.customSHA.calcSHA(verifyOtpDTO.getPassword()) + "&deviceId=" + verifyOtpDTO.getDeviceId() + "&sblUserId=" + sblUserId + "&otp=" + verifyOtpDTO.getOtp() + "&paramData=null";
        String csrf = this.util.stripNewlines(this.util.safe(this.util.getCsrf(verifyOtpDTO.getCookie())));
        if (csrf.isBlank()) {
            System.out.println("+++++ verify otp login blocked because _csrf cookie is missing from cookie jar");
            return "false";
        }
        String encryptedData = this.crypto.encrypt(loginPayLoad) + "&csrfToken=" + this.crypto.encrypt(csrf) + "&csrfDecode=false";
        System.out.println("+++++ verify otp login plain request Data is " + loginPayLoad);
        System.out.println("+++++ verify otp login encrypted request Data is " + encryptedData);
        System.out.println("+++++ verify otp login request cookie is " + verifyOtpDTO.getCookie());
        try (Response response = loginRequestExecuter(LOGIN_URL, encryptedData, verifyOtpDTO.getCookie())) {
            String responseBody = response.body() == null ? "" : response.body().string();
            System.out.println("+++++ verify otp login response code is " + response.code());
            System.out.println("+++++ verify otp login response headers are " + response.headers());
            if (!responseBody.isBlank()) {
                if (response.code() == 200) {
                    try {
                        String decrypted = this.crypto.decrypt(responseBody);
                        System.out.println("+++++ verify otp login decrypted response body is " + decrypted);
                        if (decrypted.contains("OTP is already validated")) {
                            System.out.println("+++++ returning OTP_ALREADY_VALIDATED");
                            return "OTP_ALREADY_VALIDATED";
                        }
                    }
                    catch (Exception decryptEx) {
                        System.out.println("+++++ verify otp login raw response body is " + responseBody);
                    }
                } else {
                    System.out.println("+++++ verify otp login failed body excerpt is " + this.htmlErrorExcerpt(responseBody));
                }
            }
            if (response.code() == 200) {
                String sessionId = "";
                String sessionMd5 = "";
                for (String h : response.headers("Set-Cookie")) {
                    for (HttpCookie c : HttpCookie.parse(h)) {
                        if (c.getName().equals("sessionID")) {
                            sessionId = c.getValue();
                        }
                        if (c.getName().equals("session-token-md5")) {
                            sessionMd5 = c.getValue();
                        }
                    }
                }
                // Treat HTTP 200 without these cookies as incomplete; downstream MCA calls need both.
                if (sessionId.isBlank() || sessionMd5.isBlank()) {
                    System.out.println("+++++ verify otp login did not return session cookies");
                    return "false";
                }
                String finalCookie = this.cookieUtil.updateCookies(verifyOtpDTO.getCookie(), response);
                System.out.println("+++++ verify otp login final cookie is " + finalCookie);
                return finalCookie;
            }
        }
        catch (Exception e) {
            System.out.println("+++++ verify otp login failed with exception " + e.getMessage());
        }
        return "false";
    }

    public String getRealInitialCookie() {
        Request request = new Request.Builder().url("https://www.mca.gov.in").get().header("User-Agent", "Mozilla/5.0 AppleWebKit/537.36").build();
        try {
            Response response = this.http.execute(request);
            try {
                List<String> cookies = response.headers("Set-Cookie");
                String strJoin = String.join("; ", cookies);
                if (response != null) {
                    response.close();
                }
                return strJoin;
            } finally {
            }
        } catch (Exception e) {
            log.error("Failed to fetch initial handshake cookies");
            return null;
        }
    }
}
