package com.mca.automate.service;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/* JADX INFO: loaded from: TestAPI.class */
public class TestAPI {
    public static void main(String[] args) {
        executeMasterDataRequest("deviceId=40e5c33c305; sessionID=5e29d206-97a8-4edd-b0b2-e2df7856084d; deviceId=40e5c33c305; __UUID-HASH=70ae8ff0a22335da85c8c362274ee72f; session-token-md5=qdLQ9TBl8n38/pSmwlh3XPMLiVnrizwYY4vecYH+gRgqn5lSFhDNNZ+iW6TTiCNg4EH+pcJHqzjY++zeeanf3ypd2SQ4r97CnTZW2aF8qu8df9KVPtxRpFULQDiEold6X5fSPTgmoaQ6HoHH5KIa1oeD4yyG1Tstq5jQMsGmof6wjm44/eWQZsqrZaLtGvs+VN8qqoXRwBDerzV4E09RjvEz/87awENGWh11b1nMsh3NXAKgEjKPuh9XoWUV0Y+srYO6l40DJFwK0N3wZp9suM2H4rt3MGjDL4zLn8mlFTBJEyeq1cDpDGuaIpLxq1G4IVcYgmCC9WC2yFrIv5LRvMhEIdDkKTSZxFN2ofA3KNGQk7/yKYkQp1x+S1LzSlpxuvsZGPwwJ1DgwuXU2qp9qXFgCej49n6NNcI0gsOKWrmqtrOI/PWUKk9XnU1w==; _csrf=29933644-22b2-4c14-a503-ce9b5cad8a53", "Ncz7Vx9Zd0m%2BaHHwMxUmjfef3P%2FIfXNKdDOQu%2F20vOmfi8M5K6rWNr2rE10w77Os");
    }

    public static void executeMasterDataRequest(String cookieString, String dataPayload) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create("data=" + dataPayload, MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8"));
        Request request = new Request.Builder().url("https://www.mca.gov.in/bin/MDSMasterDataServlet").post(body).addHeader("accept", "application/json, text/javascript, */*; q=0.01").addHeader("accept-language", "en-US,en;q=0.9").addHeader("origin", "https://www.mca.gov.in").addHeader("referer", "https://www.mca.gov.in/content/mca/global/en/mca/master-data/MDS/director-master-info.html?DIN=w4mWlLr%2BMmzLkwItHJUhew%3D%3D").addHeader("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").addHeader("x-requested-with", "XMLHttpRequest").addHeader("Cookie", cookieString).build();
        try {
            Response response = client.newCall(request).execute();
            try {
                System.out.println("Status Code: " + response.code());
                if (response.body() != null) {
                    System.out.println("Response: " + response.body().string());
                }
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
