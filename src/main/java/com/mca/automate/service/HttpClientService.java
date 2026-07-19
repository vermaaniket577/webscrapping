package com.mca.automate.service;

import com.mca.automate.util.McaOkHttpClientFactory;
import java.time.Duration;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

@Service
public class HttpClientService {
    private final OkHttpClient client = McaOkHttpClientFactory.newBuilder().followRedirects(true).followSslRedirects(true).connectTimeout(Duration.ofSeconds(30L)).readTimeout(Duration.ofSeconds(60L)).writeTimeout(Duration.ofSeconds(60L)).build();

    public Response execute(Request request) {
        try {
            return this.client.newCall(request).execute();
        }
        catch (Exception e) {
            throw new RuntimeException("HTTP request failed", e);
        }
    }

    public Request.Builder withBrowserHeaders(Request.Builder b) {
        return b.header("User-Agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").header("accept", "*/*").header("accept-language", "en-US,en;q=0.9").header("sec-fetch-site", "same-origin").header("sec-fetch-mode", "cors").header("sec-fetch-dest", "empty").header("origin", "https://www.mca.gov.in").header("referer", "https://www.mca.gov.in/mcafoportal/");
    }
}
