package com.mca.automate.util;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/* JADX INFO: loaded from: PrintCookie.class */
public class PrintCookie {
    public static void main(String[] args) {
    }

    public static void executeMcaLogin(String capturedCookies, String encryptedData, String encryptedCsrf) {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8");
        String bodyContent = "data=" + encryptedData + "&csrfToken=" + encryptedCsrf + "&csrfDecode=false";
        RequestBody body = RequestBody.create(bodyContent, mediaType);
        Request request = new Request.Builder().url("https://www.mca.gov.in/bin/mca/login").post(body).addHeader("accept", "*/*").addHeader("accept-language", "en-US,en;q=0.9").addHeader("content-type", "application/x-www-form-urlencoded; charset=UTF-8").addHeader("origin", "https://www.mca.gov.in").addHeader("referer", "https://www.mca.gov.in/content/mca/global/en/foportal/fologin.html").addHeader("x-requested-with", "XMLHttpRequest").addHeader("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").addHeader("Cookie", capturedCookies).build();
        try {
            Response response = client.newCall(request).execute();
            try {
                System.out.println("Login Status Code: " + response.code());
                String responseBody = response.body().string();
                System.out.println("Login Response: " + responseBody);
                System.out.println("New Session Cookies:");
                List listHeaders = response.headers("Set-Cookie");
                PrintStream printStream = System.out;
                Objects.requireNonNull(printStream);
                listHeaders.forEach(printStream::println);
                if (response != null) {
                    response.close();
                }
            } finally {
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
