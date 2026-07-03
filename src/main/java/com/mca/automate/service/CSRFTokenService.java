package com.mca.automate.service;

import lombok.Generated;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

/* JADX INFO: loaded from: CSRFTokenService.class */
@Service
public class CSRFTokenService {
    private final HttpClientService http;
    private static final String CSRF_URL = "https://www.mca.gov.in/libs/granite/csrf/token.json";

    @Generated
    public CSRFTokenService(final HttpClientService http) {
        this.http = http;
    }

    public String getCookies() {
        Request request = new Request.Builder().url(CSRF_URL).header("User-Agent", "Mozilla/5.0").header("Accept", "application/json").get().build();
        try (Response resp = this.http.execute(request)) {
            return resp.header("set-cookie");
        }
    }
}
