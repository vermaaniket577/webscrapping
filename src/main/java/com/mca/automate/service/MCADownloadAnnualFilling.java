package com.mca.automate.service;

import com.mca.automate.dto.MasterDataRequest;
import com.mca.automate.util.CryptoUtil;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/* JADX INFO: loaded from: MCADownloadAnnualFilling.class */
@Service
public class MCADownloadAnnualFilling {

    @Autowired
    CryptoUtil crypto;
    private final OkHttpClient client = new OkHttpClient();
    final String FETCH_ANNUAL_FILLING_URL = "https://www.mca.gov.in/bin/mca/services/checkAnnualFiling";
    final String DOWNLOAD_ANNUAL_FILLING_URL = "https://www.mca.gov.in/bin/mca/dms/pfmsViewDoc?mds=";

    public ResponseEntity<Resource> fetchDocumentList(MasterDataRequest mdr, String cookies) {
        String finalURL = "https://www.mca.gov.in/bin/mca/services/checkAnnualFiling?data=" + this.crypto.encrypt("CIN=" + mdr.getCompanyNameOrCIN());
        Request request = new Request.Builder().url(finalURL).get().addHeader("accept", "*/*").addHeader("accept-language", "en-US,en;q=0.9").addHeader("referer", "https://www.mca.gov.in/content/mca/global/en/application-history.html").addHeader("x-requested-with", "XMLHttpRequest").addHeader("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").addHeader("x-requested-with", "XMLHttpRequest").addHeader("Cookie", cookies).build();
        System.out.println("++++++ " + finalURL + "Cookie is " + cookies);
        try {
            Response response = this.client.newCall(request).execute();
            try {
                System.out.println("Location: " + response.header("Location"));
                if (!response.isSuccessful() || response.body() == null) {
                    ResponseEntity<Resource> responseEntityBuild = ResponseEntity.status(response.code()).build();
                    if (response != null) {
                        response.close();
                    }
                    return responseEntityBuild;
                }
                ResponseEntity<Resource> responseEntityBody = ResponseEntity.status(HttpStatus.OK).body(new ByteArrayResource(response.body().string().getBytes(StandardCharsets.UTF_8)));
                if (response != null) {
                    response.close();
                }
                return responseEntityBody;
            } finally {
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ByteArrayResource("Kindly Contact to Administrator!!!".getBytes(StandardCharsets.UTF_8)));
        }
    }

    public ResponseEntity<Resource> downloadDocuments(String dmsID) {
        return this.downloadDocuments(dmsID, "");
    }

    public ResponseEntity<Resource> downloadDocuments(String dmsID, String cookies) {
        String fullUrl = "https://www.mca.gov.in/bin/mca/dms/pfmsViewDoc?mds=" + this.crypto.encrypt(dmsID) + "&type=open&action=downloaddocument";
        Request.Builder requestBuilder = new Request.Builder().url(fullUrl).get().addHeader("accept", "*/*").addHeader("accept-language", "en-US,en;q=0.9").addHeader("referer", "https://www.mca.gov.in/content/mca/global/en/application-history.html").addHeader("x-requested-with", "XMLHttpRequest").addHeader("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").addHeader("x-requested-with", "XMLHttpRequest");
        if (cookies != null && !cookies.isBlank()) {
            requestBuilder.addHeader("Cookie", cookies);
        }
        Request request = requestBuilder.build();
        try {
            Response response = this.client.newCall(request).execute();
            try {
                System.out.println("Location: " + response.header("Location"));
                if (!response.isSuccessful() || response.body() == null) {
                    ResponseEntity<Resource> responseEntityBuild = ResponseEntity.status(response.code()).build();
                    if (response != null) {
                        response.close();
                    }
                    return responseEntityBuild;
                }
                String fileName = "download.zip";
                String contentDisposition = response.header("Content-Disposition");
                if (contentDisposition != null && contentDisposition.contains("filename=")) {
                    Matcher matcher = Pattern.compile("filename[^;=\\n]*=((['\"]).*?\\2|[^;\\n]*)").matcher(contentDisposition);
                    if (matcher.find()) {
                        fileName = matcher.group(1).replace("\"", "").replace("'", "").trim();
                    }
                }
                byte[] fileBytes = response.body().bytes();
                System.out.println(fileBytes);
                ByteArrayResource resource = new ByteArrayResource(fileBytes);
                ResponseEntity<Resource> responseEntityBody = ResponseEntity.ok().header("Content-Disposition", new String[]{"attachment; filename=\"" + fileName + "\""}).header("Content-Type", new String[]{"application/octet-stream"}).contentLength(fileBytes.length).body(resource);
                if (response != null) {
                    response.close();
                }
                return responseEntityBody;
            } finally {
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ByteArrayResource("Kindly Contact to Administrator!!!".getBytes(StandardCharsets.UTF_8)));
        }
    }
}
