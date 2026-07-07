package com.mca.automate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mca.automate.dto.ApplicationHistoryRequestDTO;
import com.mca.automate.dto.DownloadStatusDocumentDTO;
import com.mca.automate.repository.CookieRepository;
import com.mca.automate.util.CookieUtil;
import com.mca.automate.util.CryptoUtil;
import com.mca.automate.util.Util;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class MCAApplicationStatusService {
    @Autowired
    Util util;
    @Autowired
    CryptoUtil crypto;
    @Autowired
    CookieUtil cookieUtil;
    @Autowired
    CookieRepository cookieRepository;
    private static final String APP_STATUS_URL = "https://www.mca.gov.in/bin/mca/applicationHistory";
    private static final String FROM_DIR_12 = "https://www.mca.gov.in/content/forms/af/mca-forms/dir-12/dir-12/jcr:content/guideContainer.af.dermis";
    private static final MediaType FORM = MediaType.parse((String)"application/x-www-form-urlencoded");
    private static final Set<Integer> INVALID_IDS = Set.of(-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    private final OkHttpClient client = new OkHttpClient();

    public String getApplicationHistory(ApplicationHistoryRequestDTO dto, String cookies) throws IOException {
        String formData = "requestType=" + dto.getRequestType() + "&tab=" + dto.getTab() + "&cin=" + dto.getCin() + "&srnno=" + dto.getSrnno() + "&csrfToken=" + this.crypto.encrypt(this.util.stripNewlines(this.util.safe(this.util.getCsrf(cookies)))) + "&csrfdecode=false&fromDate=" + dto.getFromDate() + "&toDate=" + dto.getToDate() + "&profUser=" + dto.getProfUser();
        try (Response response = this.apiCall(formData, cookies)) {
            if (response.code() == 200) {
                return response.body() == null ? "" : response.body().string();
            }
        }
        return "Kindly Contact to Administrator!!!";
    }

    public ResponseEntity<Resource> docDownloadList(DownloadStatusDocumentDTO docDownloadList, String cookies) throws Exception {
        String dmsId = this.util.safe(docDownloadList.getDmsId());
        if (dmsId.length() > 1) {
            String finalUrl = "https://www.mca.gov.in/bin/mca/dms/dmsservicedownload?mds=" + this.crypto.encrypt(dmsId) + "&type=" + docDownloadList.getRequestType() + "&action=downloaddocument";
            return this.downloadDocuments(finalUrl, cookies);
        }
        String srn = this.crypto.encrypt(this.util.stripNewlines(this.util.safe(docDownloadList.getSrn())));
        String refNo = this.crypto.encrypt(this.util.stripNewlines(this.util.safe(docDownloadList.getReferenceNumber())));
        String formData = "requestType=" + docDownloadList.getRequestType() + "&referenceNumber=" + refNo + "&version=" + docDownloadList.getVersion() + "&srnno=" + srn + "&dscUpload=false&csrfToken=" + this.crypto.encrypt(this.util.stripNewlines(this.util.safe(this.util.getCsrf(cookies)))) + "&csrfdecode=false";
        System.out.println("++++++++ docDownloadList data is " + formData);
        try (Response response = this.apiCall(formData, cookies)) {
            if (response.code() == 200) {
                String result = response.body() == null ? "" : response.body().string();
                cookies = this.cookieUtil.updateCookies(cookies, response);
                String url = this.buildDownloadUrl(result);
                String finalUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
                if (!"".equalsIgnoreCase(url)) {
                    return this.downloadDocuments(finalUrl, cookies);
                }
            }
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ByteArrayResource("Kindly Contact to Administrator!!!".getBytes(StandardCharsets.UTF_8)));
    }

    public ResponseEntity<Resource> downloadDocuments(String fullUrl, String cookies) {
        Request request = new Request.Builder().url(fullUrl).get().addHeader("accept", "*/*").addHeader("accept-language", "en-US,en;q=0.9").addHeader("referer", "https://www.mca.gov.in/content/mca/global/en/application-history.html").addHeader("x-requested-with", "XMLHttpRequest").addHeader("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").addHeader("x-requested-with", "XMLHttpRequest").addHeader("Cookie", cookies).build();
        System.out.println("++++++ " + fullUrl + "Cookie is " + cookies);
        try (Response response = this.client.newCall(request).execute();){
            Matcher matcher;
            System.out.println("Location: " + response.header("Location"));
            if (!response.isSuccessful() || response.body() == null) {
                return ResponseEntity.status(response.code()).build();
            }
            String fileName = "download.zip";
            String contentDisposition = response.header("Content-Disposition");
            if (contentDisposition != null && contentDisposition.contains("filename=") && (matcher = Pattern.compile("filename[^;=\\n]*=((['\"]).*?\\2|[^;\\n]*)").matcher(contentDisposition)).find()) {
                fileName = matcher.group(1).replace("\"", "").replace("'", "").trim();
            }
            byte[] fileBytes = response.body().bytes();
            System.out.println(fileBytes);
            ByteArrayResource resource = new ByteArrayResource(fileBytes);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .header("Content-Type", "application/octet-stream")
                    .contentLength(fileBytes.length)
                    .body(resource);
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ByteArrayResource("Kindly Contact to Administrator!!!".getBytes(StandardCharsets.UTF_8)));
        }
    }

    public String editSRN(String refNo, String cookies) {
        String inputJson = "{\"Model0\":{\"requestBody\":{\"userId\":\"\",\"referenceNumber\":\"" + refNo + "\",\"srn\":\"\"}}}";
        String formData = "functionToExecute=invokeFDMOperation&formDataModelId=%2Fcontent%2Fdam%2Fformsanddocuments-fdm%2Fdir-12%2Fdir12-onload&input=" + URLEncoder.encode(inputJson, StandardCharsets.UTF_8) + "&operationName=" + URLEncoder.encode("POST /dir12/service/onLoad/1.0.0", StandardCharsets.UTF_8) + "&guideNodePath=" + URLEncoder.encode("/content/forms/af/mca-forms/dir-12/dir-12/jcr:content/guideContainer/rootPanel", StandardCharsets.UTF_8);
        System.out.println("++++++ edit srn data is " + formData);
        RequestBody body = RequestBody.create((String)formData, (MediaType)MediaType.parse((String)"application/x-www-form-urlencoded; charset=UTF-8"));
        Request request = new Request.Builder().url("https://www.mca.gov.in/content/forms/af/mca-forms/dir-12/dir-12/jcr:content/guideContainer.af.dermis").post(body).addHeader("accept", "text/plain, */*; q=0.01").addHeader("accept-language", "en-US,en;q=0.9").addHeader("content-type", "application/x-www-form-urlencoded; charset=UTF-8").addHeader("origin", "https://www.mca.gov.in").addHeader("referer", "https://www.mca.gov.in/content/mca/global/en/mca/e-filing/din-related-forms/form-dir12/jcr:content/root/responsivegrid/aemform.iframe.en.html?dataRef=&wcmmode=DISABLED").addHeader("x-requested-with", "XMLHttpRequest").addHeader("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").addHeader("csrf-token", "undefined").addHeader("Cookie", cookies).build();
        try (Response response = this.client.newCall(request).execute();){
            System.out.println("Status Code: " + response.code());
            String result = response.body().string();
            if (response.isSuccessful() && result != null) {
                System.out.println("+++++++resposne body is " + result);
                String string = result;
                return string;
            }
            System.out.println("Failed to invoke DIR-12 onLoad. Check if session-token-md5 is expired.");
            return "Kindly Contact to Administrator!!!";
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return "Kindly Contact to Administrator!!!";
    }

    public Response apiCall(String formData, String cookies) throws IOException {
        RequestBody body = RequestBody.create((String)formData, (MediaType)MediaType.parse((String)"application/x-www-form-urlencoded; charset=UTF-8"));
        Request request = new Request.Builder().url("https://www.mca.gov.in/bin/mca/applicationHistory").post(body).addHeader("accept", "*/*").addHeader("accept-language", "en-US,en;q=0.9").addHeader("content-type", "application/x-www-form-urlencoded; charset=UTF-8").addHeader("origin", "https://www.mca.gov.in").addHeader("referer", "https://www.mca.gov.in/content/mca/global/en/application-history.html").addHeader("x-requested-with", "XMLHttpRequest").addHeader("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").addHeader("Cookie", cookies).build();
        Response response = this.client.newCall(request).execute();
        return response;
    }

    public String buildDownloadUrl(String jsonResponse) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(jsonResponse);
        String resCode = root.path("resCode").asText();
        if (!"200".equals(resCode)) {
            return "ERROR: Invalid response code";
        }
        JsonNode attachmentArray = root.path("attachmentDMSId");
        if (!attachmentArray.isArray() || attachmentArray.size() == 0) {
            return "ERROR: No attachment DMS IDs found";
        }
        for (JsonNode node : attachmentArray) {
            String dmsId = node.asText();
            if (!MCAApplicationStatusService.isInvalidDmsId(dmsId)) continue;
            return "PDF generation in progress. Please refresh / revisit this page and check after sometime";
        }
        ArrayList<String> dmsIds = new ArrayList<String>();
        for (JsonNode node : attachmentArray) {
            dmsIds.add(node.asText());
        }
        String joinedDmsIds = String.join(":", dmsIds);
        String encryptedValue = this.crypto.encrypt(joinedDmsIds);
        String baseUrl = "https://www.mca.gov.in";
        String fullUrl = "";
        if (attachmentArray.size() > 1) {
            fullUrl = baseUrl + "/bin/mca/dms/dmsdownloadmultipledocuments?mds=" + URLEncoder.encode(encryptedValue, StandardCharsets.UTF_8) + "&type=download&action=downloaddocument";
        }
        if (attachmentArray.size() == 1) {
            fullUrl = baseUrl + "/bin/mca/dms/dmsservicedownload?mds=" + URLEncoder.encode(encryptedValue, StandardCharsets.UTF_8) + "&type=download&action=downloaddocument";
        }
        return fullUrl;
    }

    public static boolean isInvalidDmsId(String dmsId) {
        try {
            int id = Integer.parseInt(dmsId.trim());
            return INVALID_IDS.contains(id);
        }
        catch (Exception e) {
            return true;
        }
    }
}
