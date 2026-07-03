package com.mca.automate.service;

import com.mca.automate.dto.VerifyOtpDTO;
import com.mca.automate.util.CookieUtil;
import com.mca.automate.util.CryptoUtil;
import java.io.IOException;
import lombok.Generated;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/* JADX INFO: loaded from: OtpService.class */
@Service
public class OtpService {

    @Autowired
    CookieService cookieService;

    @Autowired
    CryptoUtil crypto;

    @Autowired
    CookieUtil cookieUtil;
    private final HttpClientService http;
    final String VERIFY_OTP_URL = "https://www.mca.gov.in/bin/verifyOTP";
    final String SEND_OTP_URL = "https://www.mca.gov.in/bin/sendOTP";
    private static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded");

    @Generated
    public OtpService(final HttpClientService http) {
        this.http = http;
    }

    public String verifOtp(VerifyOtpDTO verifyOtpDTO) throws IOException {
        String cookie = verifyOtpDTO.getCookie();
        String plain = "requestType=sameOTP&EmailId=" + verifyOtpDTO.getEmail().toUpperCase() + "&mobileNo=" + verifyOtpDTO.getMobile() + "&otp=" + verifyOtpDTO.getOtp();
        System.out.println("+++++++++++++ verify otp data is " + plain);
        System.out.println("+++++++++++++ verify otp cookie is " + cookie);
        String encrypted = this.crypto.encrypt(plain);
        System.out.println("+++++++++++++ verify otp data is " + encrypted);
        RequestBody body = RequestBody.create("data=" + encrypted, FORM);
        Request req = new Request.Builder().url("https://www.mca.gov.in/bin/verifyOTP").post(body).header("accept", "*/*").header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8").header("origin", "https://www.mca.gov.in").header("referer", "https://www.mca.gov.in/content/mca/global/en/foportal/fologin.html").header("sec-fetch-dest", "empty").header("sec-fetch-mode", "cors").header("sec-fetch-site", "same-origin").header("x-requested-with", "XMLHttpRequest").header("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").header("Cookie", cookie).build();
        try (Response resp = this.http.execute(req)) {
            String cookie2 = this.cookieUtil.updateCookies(cookie, resp);
            String responseBody = resp.body() == null ? "" : resp.body().string();
            System.out.println("+++++++++++ verify otp response code is " + resp.code() + "++++++++++ verify otp response body is " + responseBody);
            if (resp.code() != 200) {
                return "failed";
            }
            return cookie2;
        }
    }

    public String sendOtp(String mobileNo, String emailId, String cookies, String requestType, String template) throws IOException {
        System.out.println("Inside send otp");
        String sendOtpPayload = "requestType=" + requestType + "&mobileNo=" + mobileNo + "&EmailId=" + emailId + "&templateType=" + template + "&countryName=India&specificType=login";
        System.out.println("+++++++++++++ send otp data is " + sendOtpPayload);
        System.out.println("+++++++++++++ send otp cookie is " + cookies);
        String encrypted = this.crypto.encrypt(sendOtpPayload);
        System.out.println("+++++++++++++ send otp data is " + encrypted);
        RequestBody body = RequestBody.create("data=" + encrypted, FORM);
        Request req = new Request.Builder().url("https://www.mca.gov.in/bin/sendOTP").post(body).header("accept", "*/*").header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8").header("origin", "https://www.mca.gov.in").header("referer", "https://www.mca.gov.in/content/mca/global/en/foportal/fologin.html").header("sec-fetch-dest", "empty").header("sec-fetch-mode", "cors").header("sec-fetch-site", "same-origin").header("x-requested-with", "XMLHttpRequest").header("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").header("Cookie", cookies).build();
        try (Response resp = this.http.execute(req)) {
            String responseBody = resp.body() == null ? "" : resp.body().string();
            System.out.println("+++++++++++ send otp response code is " + resp.code() + "++++++++++ send opt response body is " + responseBody);
            int statusCode = resp.code();
            if (statusCode == 200) {
                return this.cookieUtil.updateCookies(cookies, resp);
            }
        }
        return "failed";
    }
}
