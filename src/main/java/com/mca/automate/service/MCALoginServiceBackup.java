package com.mca.automate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.mca.automate.dto.CookieEntity;
import com.mca.automate.dto.CookieForSession;
import com.mca.automate.dto.LoginResponse;
import com.mca.automate.dto.ValidateCaptchaResponse;
import com.mca.automate.repository.CookieRepository;
import com.mca.automate.util.CookieUtil;
import com.mca.automate.util.CryptoUtil;
import com.mca.automate.util.CustomSHA;
import com.mca.automate.util.Util;
import java.net.HttpCookie;
import java.util.Iterator;
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

/* JADX INFO: loaded from: MCALoginServiceBackup.class */
@Service
public class MCALoginServiceBackup {
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
    private static final String LOGIN_URL = "https://www.mca.gov.in/bin/mca/login";
    private static final String OTP_URL = "https://www.mca.gov.in/bin/sendOTP";

    @Generated
    private static final Logger log = LoggerFactory.getLogger(MCALoginServiceBackup.class);
    private static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded");

    @Generated
    public MCALoginServiceBackup(final HttpClientService http, final CryptoUtil crypto) {
        this.http = http;
        this.crypto = crypto;
    }

    public LoginResponse login(String username, String password) {
        try {
            String deviceId = this.userRegistration.getDeviceId(username);
            if (null == deviceId || deviceId.length() < 1) {
                return new LoginResponse("NU", "New User Registration Required", false);
            }
            CookieForSession cookie = this.cookieService.getCookiesWithType(username);
            if (cookie != null && "final".equalsIgnoreCase(cookie.getCookieType()) && cookie.getCookies().length() > 0) {
                System.out.println("Using existing cookie");
                return new LoginResponse(cookie.getCookies(), "message: Login Successful", true);
            }
            if (cookie != null && "otp".equalsIgnoreCase(cookie.getCookieType()) && cookie.getCookies().length() > 0) {
                String plain = "requestType=otpVerified&userName=" + username.toUpperCase() + "&password=" + cookie.getPassword() + "&deviceId=" + this.util.extractDeviceId(cookie.getCookies()) + "&clientIp=\u009eée&paramData=null";
                String encrypted = this.crypto.encrypt(plain) + "&csrfToken=" + this.crypto.encrypt(this.util.getCsrf(cookie.getCookies())) + "&csrfDecode=false";
                Response resp = loginRequestExecuter(LOGIN_URL, encrypted, cookie.getCookies());
                int responseCode = resp.code();
                System.out.println("++++++" + responseCode);
                if (responseCode == 200) {
                    String sessionId = "";
                    String sessionMd5 = "";
                    for (String h : resp.headers("Set-Cookie")) {
                        for (HttpCookie c : HttpCookie.parse(h)) {
                            if (c.getName().equals("sessionID")) {
                                sessionId = c.getValue();
                            }
                            if (c.getName().equals("session-token-md5")) {
                                sessionMd5 = c.getValue();
                            }
                        }
                    }
                    if ("".equalsIgnoreCase(sessionId) || "".equalsIgnoreCase(sessionMd5)) {
                        return new LoginResponse("", "Login Failed please connect to Administrator", false);
                    }
                    String finalCookie = cookie.getCookies() + ";session-token-md5=" + sessionMd5 + ";sessionID=" + sessionId;
                    CookieEntity cookieEntity = new CookieEntity();
                    cookieEntity.setEmailId(username);
                    cookieEntity.setCookies(finalCookie);
                    cookieEntity.setCookieType("final");
                    cookieEntity.setMobileNo(cookie.getMobile());
                    cookieEntity.setPassword(cookie.getPassword());
                    cookieEntity.setDeviceId(deviceId);
                    cookieEntity.setUpdatedAt(System.currentTimeMillis());
                    this.cookieRepository.save(cookieEntity);
                    resp.close();
                    return new LoginResponse(finalCookie, "message: Login Successful", true);
                }
                return new LoginResponse("", "message: Login Failed please connect to Administrator", false);
            }
            ValidateCaptchaResponse cap = this.mcaCaptchaService.captchaValidatonWrapper("");
            if (!cap.status()) {
                System.out.println("Captcha Validation Failed......");
                return new LoginResponse("", "message: Login Failed please connect to Administrator", false);
            }
            String hashedPassword = this.customSHA.calcSHA(password);
            String plain2 = "requestType=login&userName=" + username + "&password=" + hashedPassword + "&deviceId=" + deviceId + "&clientIp=\u009eée&userInput=" + cap.captcha() + "&pre_CT=" + cap.preCt() + "&paramData=null";
            String encrypted2 = this.crypto.encrypt(plain2) + "&csrfToken=" + this.crypto.encrypt(this.util.getCsrf(cap.cookie())) + "&csrfDecode=false";
            Response resp2 = loginRequestExecuter(LOGIN_URL, encrypted2, cap.cookie());
            System.out.println("Login Code is " + resp2.code());
            String loginResonse = resp2.body().string();
            System.out.println("Login  cookie is " + cap.cookie());
            if (resp2.code() != 200) {
                resp2.close();
                return new LoginResponse("", "message: Login Failed please connect to Administrator", false);
            }
            String sessionId2 = "";
            String sessionMd52 = "";
            String responseBody = this.crypto.decrypt(loginResonse);
            System.out.println("++++Login response is++++ " + responseBody);
            JsonMapper jsonMapper = new JsonMapper();
            JsonNode rootNode = jsonMapper.readTree(responseBody);
            String resCode = rootNode.get("resCode").asText();
            if ("206".equalsIgnoreCase(resCode)) {
                String resStr = rootNode.path("resStr").asText();
                JsonNode innerNode = jsonMapper.readTree(resStr);
                JsonNode dataNode = innerNode.path("data");
                String email = dataNode.path("email").asText();
                String mobile = dataNode.path("mobile").asText();
                String str = "requestType=onlyEmailOTP&EmailId=" + email + "&templateType=New_Device_Notify&mobileNo=undefined";
                String planDataForMobile = "requestType=sameOTP&mobileNo=" + mobile + "&EmailId=" + email + "&templateType=User_Login&countryName=India&specificType=login";
                String encryptedData = this.crypto.encrypt(planDataForMobile);
                System.out.println("+++++++ otp request data is " + encryptedData);
                Response otpResponse = loginRequestExecuter(OTP_URL, encryptedData, cap.cookie());
                System.out.println("+++++++ otp response code is " + otpResponse.code());
                String loginOtpRow = "";
                List<String> allCookies = otpResponse.headers("Set-Cookie");
                Iterator<String> it = allCookies.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    String header = it.next();
                    if (header.contains("login_sameOTPRowid")) {
                        loginOtpRow = header.split(";")[0];
                        System.out.println("+++++ Inside for loop otp RowId " + loginOtpRow);
                        break;
                    }
                }
                String cookieWithOtpRowID = cap.cookie() + "; " + loginOtpRow;
                CookieEntity cookieEntity2 = new CookieEntity();
                cookieEntity2.setEmailId(username);
                cookieEntity2.setCookies(cookieWithOtpRowID);
                cookieEntity2.setCookieType("otp");
                cookieEntity2.setPassword(hashedPassword);
                cookieEntity2.setMobileNo(mobile);
                cookieEntity2.setDeviceId(deviceId);
                cookieEntity2.setUpdatedAt(System.currentTimeMillis());
                this.cookieRepository.save(cookieEntity2);
                otpResponse.close();
                return new LoginResponse("Otp Required", "Please Verify OTP", true);
            }
            if ("208".equalsIgnoreCase(resCode)) {
                System.out.println("+++++ inside 208+++++++++");
                String plainForceLogin = "requestType=login&previousSession=killActiveSession&userName=" + username + "&password=" + hashedPassword + "&userInput=" + cap.captcha() + "&deviceId=" + deviceId + "&clientIp=\u009eée&pre_CT=" + cap.preCt() + "&paramData=null";
                Response forceLoginResponse = loginRequestExecuter(LOGIN_URL, this.crypto.encrypt(plainForceLogin) + "&csrfToken=" + this.crypto.encrypt(this.util.getCsrf(cap.cookie())) + "&csrfDecode=false", cap.cookie());
                System.out.println("+++++ inside 208+++++++++ response code is " + forceLoginResponse.code());
                for (String h2 : forceLoginResponse.headers("Set-Cookie")) {
                    for (HttpCookie c2 : HttpCookie.parse(h2)) {
                        if (c2.getName().equals("sessionID")) {
                            sessionId2 = c2.getValue();
                        }
                        if (c2.getName().equals("session-token-md5")) {
                            sessionMd52 = c2.getValue();
                        }
                    }
                }
                if ("".equalsIgnoreCase(sessionId2) || "".equalsIgnoreCase(sessionMd52)) {
                    return new LoginResponse("", "message: Login Failed please connect to Administrator", false);
                }
                String finalCookie2 = cap.cookie() + ";session-token-md5=" + sessionMd52 + ";sessionID=" + sessionId2;
                CookieEntity cookieEntity3 = new CookieEntity();
                cookieEntity3.setEmailId(username);
                cookieEntity3.setCookies(finalCookie2);
                cookieEntity3.setCookieType("final");
                cookieEntity3.setDeviceId(deviceId);
                cookieEntity3.setUpdatedAt(System.currentTimeMillis());
                this.cookieRepository.save(cookieEntity3);
                forceLoginResponse.close();
                return new LoginResponse(finalCookie2, "Login Successful", true);
            }
            for (String h3 : resp2.headers("Set-Cookie")) {
                for (HttpCookie c3 : HttpCookie.parse(h3)) {
                    if (c3.getName().equals("sessionID")) {
                        sessionId2 = c3.getValue();
                    }
                    if (c3.getName().equals("session-token-md5")) {
                        sessionMd52 = c3.getValue();
                    }
                }
            }
            if ("".equalsIgnoreCase(sessionId2) || "".equalsIgnoreCase(sessionMd52)) {
                resp2.close();
                return new LoginResponse("", "message: Login Failed please connect to Administrator", false);
            }
            String finalCookie3 = cap.cookie() + ";session-token-md5=" + sessionMd52 + ";sessionID=" + sessionId2;
            CookieEntity cookieEntity4 = new CookieEntity();
            cookieEntity4.setEmailId(username);
            cookieEntity4.setCookies(finalCookie3);
            cookieEntity4.setCookieType("final");
            cookieEntity4.setDeviceId(deviceId);
            cookieEntity4.setUpdatedAt(System.currentTimeMillis());
            this.cookieRepository.save(cookieEntity4);
            resp2.close();
            return new LoginResponse(finalCookie3, "message: Login Successful", true);
        } catch (Exception e) {
            log.error("Login failed", e);
            return new LoginResponse("", "message: Login Failed please connect to Administrator", false);
        }
    }

    public Response loginRequestExecuter(String URL, String encrypted, String cookie) {
        RequestBody body = RequestBody.create("data=" + encrypted, FORM);
        Request req = new Request.Builder().url(URL).post(body).header("accept", "*/*").header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8").header("origin", "https://www.mca.gov.in").header("referer", "https://www.mca.gov.in/content/mca/global/en/foportal/fologin.html").header("sec-fetch-dest", "empty").header("sec-fetch-mode", "cors").header("sec-fetch-site", "same-origin").header("x-requested-with", "XMLHttpRequest").header("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").header("Cookie", cookie).build();
        Response resp = this.http.execute(req);
        return resp;
    }
}
