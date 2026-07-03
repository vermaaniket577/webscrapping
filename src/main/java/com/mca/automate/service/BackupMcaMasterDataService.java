package com.mca.automate.service;

import com.mca.automate.util.CookieUtil;
import com.mca.automate.util.CryptoUtil;
import com.mca.automate.util.JsonUtil;
import java.io.IOException;
import lombok.Generated;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

/* JADX INFO: loaded from: BackupMcaMasterDataService.class */
@Service
public class BackupMcaMasterDataService {
    private final HttpClientService http;
    private final CookieUtil cookieUtil;
    private final CryptoUtil cryptoUtil;
    private final JsonUtil jsonUtil;
    private static final String MASTER_URL = "https://www.mca.gov.in/bin/MDSMasterDataServlet";
    private static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded");

    @Generated
    public BackupMcaMasterDataService(final HttpClientService http, final CookieUtil cookieUtil, final CryptoUtil cryptoUtil, final JsonUtil jsonUtil) {
        this.http = http;
        this.cookieUtil = cookieUtil;
        this.cryptoUtil = cryptoUtil;
        this.jsonUtil = jsonUtil;
    }

    public String fetchMaster(String md5, String sessionId, String searchJson, String preCt, String captcha) throws IOException {
        String cin = this.jsonUtil.extractCin(searchJson);
        String plain = "ID=" + cin + "&requestID=cin&userInput=" + captcha + "&pre_CT=" + preCt;
        String encrypted = this.cryptoUtil.encrypt(plain);
        String body = "data=" + encrypted;
        Request req = this.http.withBrowserHeaders(new Request.Builder().url(MASTER_URL).header("header", "").post(RequestBody.create(body, FORM))).build();
        try (Response resp = this.http.execute(req)) {
            return resp.body() == null ? "" : resp.body().string();
        }
    }

    public String fetchDin(String md5, String sessionId, String din) throws IOException {
        String plain = "ID=" + din + "&requestID=din&userInput=&pre_CT=";
        String encrypted = this.cryptoUtil.encrypt(plain);
        String body = "data=" + encrypted;
        Request req = this.http.withBrowserHeaders(new Request.Builder().url(MASTER_URL).header("header", "").post(RequestBody.create(body, FORM))).build();
        try (Response resp = this.http.execute(req)) {
            return resp.body() == null ? "" : resp.body().string();
        }
    }
}
