package com.mca.automate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mca.automate.dto.DinPanVerifyResponse;
import com.mca.automate.dto.LoginResponse;
import com.mca.automate.dto.MasterDataRequest;
import com.mca.automate.dto.ValidateCaptchaResponse;
import com.mca.automate.util.CookieUtil;
import com.mca.automate.util.CryptoUtil;
import com.mca.automate.util.Util;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Generated;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/* JADX INFO: loaded from: McaSearchService.class */
@Service
public class McaSearchService {
    private final HttpClientService http;
    private final CryptoUtil crypto;
    private final Util util;
    private final McaCaptchaService captchaService;
    private final CookieService cookieService;
    private final CookieUtil cookieUtil;
    private static final String SEARCH_URL = "https://www.mca.gov.in/bin/mca/mds/commonSearch";
    private static final String CHECK_DIN_STATUS_URL = "https://www.mca.gov.in/bin/mca/foservicedinstatus?data=";
    private static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8");
    // TODO CONFIRM: this is the servlet the "Verify DIN/DPIN-PAN Details of
    // Director" page calls
    // when the second Submit button (the one next to the PAN field) is clicked. It
    // is inferred
    // from the naming convention MCA uses for its other "foservice*" GET endpoints
    // (see
    // CHECK_DIN_STATUS_URL above) but it has NOT been confirmed against a real
    // captured request.
    // To confirm/replace it: open Chrome DevTools -> Network -> XHR/Fetch filter,
    // open
    // https://www.mca.gov.in/content/mca/global/en/mca/fo-llp-services/verify-din-pan-details-of-director.html,
    // submit the DIN box, then submit the PAN box, and copy the exact request URL +
    // form/query
    // params for the second call. Update VERIFY_DIN_PAN_URL and the plain-text
    // payload built in
    // verifyDinPan() below to match exactly what you see there.
    private static final String VERIFY_DIN_PAN_URL = "https://www.mca.gov.in/bin/mca/dinpanValidate?data=";

    // Literal message fragments MCA's own page displays. Confirmed against the live
    // page copy
    // (case-insensitive substring match is used so minor whitespace/HTML
    // differences don't matter).
    private static final String MSG_MATCHING = "din details are matching with the income tax pan database of mca";
    private static final String MSG_NOT_MATCHING = "din details are not matching with the income tax pan database";
    private static final String MSG_PAN_NOT_MATCHING = "does not match with the income tax pan existing";
    private static final String MSG_PAN_NOT_EXIST = "income tax pan does not exist in the din database";
    private static final String MSG_INVALID_DIN = "please enter a valid and approved din";

    @Autowired
    public McaSearchService(final HttpClientService http, final CryptoUtil crypto, final Util util,
            final McaCaptchaService captchaService, final CookieService cookieService, final CookieUtil cookieUtil) {
        this.http = http;
        this.crypto = crypto;
        this.util = util;
        this.captchaService = captchaService;
        this.cookieService = cookieService;
        this.cookieUtil = cookieUtil;
    }

    public String search(MasterDataRequest masterDataRequest, LoginResponse loginResponse,
            ValidateCaptchaResponse cResponse) throws IOException {
        System.out.println("inside simple search " + cResponse.captcha() + "===" + cResponse.preCt());
        String plain = "module=MDS&searchKeyWord=" + masterDataRequest.getCompanyNameOrCIN()
                + "&searchType=autosuggest&mdsSearchType=company&userInput=" + cResponse.captcha() + "&pre_CT="
                + cResponse.preCt();
        System.out.println("plain body is for simple search " + plain);
        String cookie = this.latestCookie(loginResponse, cResponse);
        String csrf = this.util.getCsrf(cookie);
        if (csrf == null || csrf.isBlank()) {
            System.out.println("MDS commonSearch request blocked because _csrf cookie is missing");
            return "Missing request parameter!!!";
        }
        String encrypted = this.crypto.encrypt(plain);
        String body = "data=" + encrypted + "&csrfToken=" + this.crypto.encrypt(csrf) + "&csrfDecode=false";
        Request req = new Request.Builder().url(SEARCH_URL).post(RequestBody.create(body, FORM))
                .header("cookie", cookie).header("accept", "application/json, text/javascript, */*; q=0.01")
                .header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8").header("origin", "https://www.mca.gov.in")
                .header("referer", "https://www.mca.gov.in/content/mca/global/en/mca/master-data/MDS.html")
                .header("sec-fetch-dest", "empty").header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-origin").header("x-requested-with", "XMLHttpRequest")
                .header("user-agent",
                        "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36")
                .build();
        try (Response resp = this.http.execute(req)) {
            String respBody = resp.body() == null ? "" : resp.body().string();
            System.out.println(" Responsebody is for simple search " + respBody);
            return respBody;
        }
    }

    public String checkCompanyName(MasterDataRequest masterDataRequest, LoginResponse loginResponse,
            ValidateCaptchaResponse cResponse) throws IOException {
        System.out.println("inside simple search " + cResponse.captcha() + "===" + cResponse.preCt());
        String plain = "module=NameSearch&searchKeyWord=" + this.crypto.encrypt(masterDataRequest.getCompanyNameOrCIN())
                + "&searchType=autosuggest&mdsSearchType=searchedName&userInput=" + cResponse.captcha() + "&pre_CT="
                + cResponse.preCt();
        System.out.println("plain body is for simple search " + plain);
        String cookie = this.latestCookie(loginResponse, cResponse);
        String csrf = this.util.getCsrf(cookie);
        if (csrf == null || csrf.isBlank()) {
            System.out.println("NameSearch commonSearch request blocked because _csrf cookie is missing");
            return "Missing request parameter!!!";
        }
        String encrypted = this.crypto.encrypt(plain);
        String body = "data=" + encrypted + "&csrfToken=" + this.crypto.encrypt(csrf) + "&csrfDecode=false";
        Request req = new Request.Builder().url(SEARCH_URL).post(RequestBody.create(body, FORM))
                .header("cookie", cookie).header("accept", "*/*")
                .header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8").header("origin", "https://www.mca.gov.in")
                .header("referer",
                        "https://www.mca.gov.in/content/mca/global/en/mca/fo-llp-services/company-llp-name-search.html")
                .header("sec-fetch-dest", "empty").header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-origin").header("x-requested-with", "XMLHttpRequest")
                .header("user-agent",
                        "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36")
                .build();
        try (Response resp = this.http.execute(req)) {
            String respBody = resp.body() == null ? "" : resp.body().string();
            System.out.println(" Responsebody is for simple search " + respBody);
            return respBody;
        }
    }

    public String searchMasterData(String requestedIdentifier, LoginResponse loginResponse, String searchResponse,
            String requestType) throws Exception {
        ValidateCaptchaResponse cap = this.captchaService.captchaValidatonWrapper(loginResponse.cookie());
        if (!cap.status()) {
            return "Missing request parameter!!!";
        }
        String cookieHeader = this.latestCookie(loginResponse, cap);
        String csrf = this.util.getCsrf(cookieHeader);
        if (csrf == null || csrf.isBlank()) {
            System.out.println("MDS master-data request blocked because _csrf cookie is missing");
            return "Missing request parameter!!!";
        }
        String plainData = "ID=" + searchResponse + "&requestID=din&userInput=" + cap.captcha() + "&pre_CT="
                + cap.preCt();
        String data = "data=" + this.crypto.encrypt(plainData) + "&csrfToken=" + this.crypto.encrypt(csrf)
                + "&csrfDecode=false";
        if ("cin".equalsIgnoreCase(requestType)) {
            // Autosuggest may return multiple companies; prefer the caller's exact CIN when
            // present.
            String cnNumber = this.util.extractCIN(searchResponse, requestedIdentifier);
            String plainData2 = "ID=" + cnNumber + "&requestID=cin&userInput=" + cap.captcha() + "&pre_CT="
                    + cap.preCt();
            data = "data=" + this.crypto.encrypt(plainData2) + "&csrfToken=" + this.crypto.encrypt(csrf)
                    + "&csrfDecode=false";
        }
        System.out.println("inside main Request Body is " + data);
        System.out.println("inside main cookieHeader " + cookieHeader);
        Request req = new Request.Builder().url("https://www.mca.gov.in/bin/MDSMasterDataServlet")
                .header("accept", "application/json, text/javascript, */*; q=0.01")
                .header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8")
                .header("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("cookie", cookieHeader).header("origin", "https://www.mca.gov.in")
                .header("referer", "https://www.mca.gov.in/content/mca/global/en/mca/master-data/MDS.html")
                .header("user-agent",
                        "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36")
                .header("x-requested-with", "XMLHttpRequest").post(RequestBody.create(data, FORM)).build();
        try (Response resp = this.http.execute(req)) {
            System.out.println("-----------------" + resp.code());
            String respo = resp.body() == null ? "" : resp.body().string();
            System.out.println("Final Search Response body is-----------------" + respo);
            return respo;
        }
    }

    public String checkDinStatus(String din) throws Exception {
        ValidateCaptchaResponse cap = this.captchaService.captchaValidatonWrapper("");
        if (!cap.status()) {
            return "Missing request parameter!!!";
        }
        return checkDinStatusWithSession(din, cap);
    }

    private String checkDinStatusWithSession(String din, ValidateCaptchaResponse cap) throws Exception {
        String data = "DIN=" + din + "&userInput=" + cap.captcha() + "&pre_CT=" + cap.preCt();
        String finalURL = "https://www.mca.gov.in/bin/mca/foservicedinstatus?data=" + this.crypto.encrypt(data);
        Request request = new Request.Builder().url(finalURL).get().addHeader("accept", "*/*")
                .addHeader("accept-language", "en-US,en;q=0.9")
                .addHeader("referer", "https://www.mca.gov.in/content/mca/global/en/application-history.html")
                .addHeader("x-requested-with", "XMLHttpRequest")
                .addHeader("user-agent",
                        "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36")
                .addHeader("cookie", cap.cookie()).build();
        try (Response resp = this.http.execute(request)) {
            if (resp.code() != 200) {
                return "Internal Error Occured";
            }
            return resp.body() == null ? "" : resp.body().string();
        }
    }

    /**
     * Mirrors the two-step "Verify DIN/DPIN-PAN Details of Director" flow on
     * mca.gov.in:
     * 1) DIN alone -> look up the director's name and check status on session.
     * 2) DIN + PAN -> ask MCA whether the PAN matches its DIN database on same session.
     */
    public DinPanVerifyResponse verifyDinPan(String din, String pan) {
        try {
            // 1. First step: solve captcha and call foservicedinprefill to initialize the session and obtain Director Name
            ValidateCaptchaResponse cap1 = captchaService.captchaValidatonWrapper("");
            if (!cap1.status()) {
                return new DinPanVerifyResponse("ERROR", din, pan, null,
                        "Verification failed. Captcha could not be solved for initial DIN check");
            }

            String dinStatusResponse = checkDinPrefillWithSession(din, cap1);
            System.out.println("checkDinPrefillWithSession response: " + dinStatusResponse);
            String directorName = extractDirectorName(dinStatusResponse);
            if (dinStatusResponse.contains("Internal Error Occured") || dinStatusResponse.contains("Invalid DIN") || dinStatusResponse.contains("Expired") || dinStatusResponse.contains("Incorrect")) {
                return new DinPanVerifyResponse("INVALID_DIN", din, pan, directorName,
                        "DIN verification check failed: " + dinStatusResponse);
            }

            // 2. Second step: call PAN verify directly on the exact same session cookie.
            DinPanVerifyResponse result = callDinPanVerify(din, pan, directorName, cap1.cookie());
            if ("ERROR_RETRY".equals(result.status())) {
                ValidateCaptchaResponse capRetry = captchaService.captchaValidatonWrapper("");
                if (capRetry.status()) {
                    String dinRetryResponse = checkDinPrefillWithSession(din, capRetry);
                    directorName = extractDirectorName(dinRetryResponse);
                    result = callDinPanVerify(din, pan, directorName, capRetry.cookie());
                }
            }
            if ("ERROR_RETRY".equals(result.status())) {
                return new DinPanVerifyResponse("ERROR", din, pan, directorName, result.message());
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new DinPanVerifyResponse("ERROR", din, pan, null,
                    "Verification failed. Exception: " + e.getMessage());
        }
    }

    private String checkDinPrefillWithSession(String din, ValidateCaptchaResponse cap) throws Exception {
        String requestData = "id=" + din + "&userInput=" + cap.captcha() + "&pre_CT=" + cap.preCt();
        String csrfToken = this.util.getCsrf(cap.cookie());
        String dataParam = "data=" + this.crypto.encrypt(requestData);
        if (csrfToken != null && !csrfToken.isBlank()) {
            dataParam += "&csrfToken=" + this.crypto.encrypt(csrfToken);
        }
        String finalURL = "https://www.mca.gov.in/bin/mca/foservicedinprefill?" + dataParam;
        Request.Builder reqBuilder = new Request.Builder().url(finalURL).get()
                .header("accept", "application/json, text/javascript, */*; q=0.01")
                .header("accept-language", "en-US,en;q=0.9")
                .header("referer", "https://www.mca.gov.in/content/mca/global/en/mca/fo-llp-services/verify-din-pan-details-of-director.html")
                .header("x-requested-with", "XMLHttpRequest")
                .header("user-agent",
                        "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36")
                .header("cookie", cap.cookie());
        if (csrfToken != null && !csrfToken.isBlank()) {
            reqBuilder.header("x-csrf-token", csrfToken);
        }
        try (Response resp = this.http.execute(reqBuilder.build())) {
            if (resp.code() != 200) {
                return "Internal Error Occured (HTTP " + resp.code() + ")";
            }
            return resp.body() == null ? "" : resp.body().string();
        }
    }

    /**
     * Single attempt at the DIN+PAN verify call. ALWAYS reads MCA's response body
     * (even on non-2xx) so callers can see exactly why MCA rejected the request.
     * Returns a sentinel "ERROR_RETRY" status on 401/403 so the caller can retry
     * with a fresh session; any other failure becomes a permanent "ERROR".
     */
    private DinPanVerifyResponse callDinPanVerify(String din, String pan, String directorName, String sessionCookie)
            throws Exception {
        // Build payload with DIN and PAN exactly as observed in JS verifypan()
        String payload = "DIN=" + din + "&PAN=" + pan;
        String encryptedData = crypto.encrypt(payload);
        String finalUrl = VERIFY_DIN_PAN_URL + encryptedData;

        String csrfToken = this.util.getCsrf(sessionCookie);
        Request.Builder reqBuilder = new Request.Builder()
                .url(finalUrl)
                .get()
                .header("cookie", sessionCookie)
                .header("accept", "application/json, text/javascript, */*; q=0.01")
                .header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8")
                .header("referer",
                        "https://www.mca.gov.in/content/mca/global/en/mca/fo-llp-services/verify-din-pan-details-of-director.html")
                .header("x-requested-with", "XMLHttpRequest")
                .header("user-agent",
                        "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36");
        if (csrfToken != null && !csrfToken.isBlank()) {
            reqBuilder.header("x-csrf-token", csrfToken);
        }

        try (Response response = http.execute(reqBuilder.build())) {
            String responseBody = response.body() == null ? "" : response.body().string();
            int code = response.code();
            System.out.println("verifyDinPan MCA HTTP " + code + " body=" + responseBody);

            if (code == 401 || code == 403) {
                // Session/CSRF rejected - let the caller retry once with a fresh session.
                return new DinPanVerifyResponse("ERROR_RETRY", din, pan, directorName,
                        "MCA returned HTTP " + code + " (session rejected). Body: " + responseBody);
            }
            if (code != 200) {
                return new DinPanVerifyResponse("ERROR", din, pan, directorName,
                        "MCA returned HTTP " + code + ". Body: " + responseBody);
            }

            String lowerBody = responseBody.toLowerCase();
            String dynamicStatus = null;
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(responseBody);
                JsonNode dataNode = root.path("data");
                if (!dataNode.isMissingNode() && !dataNode.path("status").isMissingNode()) {
                    dynamicStatus = dataNode.path("status").asText().trim();
                } else if (!root.path("status").isMissingNode()) {
                    dynamicStatus = root.path("status").asText().trim();
                }
            } catch (Exception e) {
                // ignore parsing exceptions
            }

            if (lowerBody.contains("\"status\":\"true\"") || lowerBody.contains("\"status\": \"true\"") || lowerBody.contains("status: \"true\"") || lowerBody.contains("status=\"true\"") || lowerBody.contains(MSG_MATCHING) || lowerBody.contains("matching")) {
                return new DinPanVerifyResponse("matched", din, pan, directorName,
                        "DIN details are matching with the Income tax PAN database of MCA");
            } else if (lowerBody.contains("entered pan does not exist") || lowerBody.contains("does not exist") || lowerBody.contains(MSG_PAN_NOT_EXIST)) {
                return new DinPanVerifyResponse("not matched", din, pan, directorName,
                        "Income tax PAN does not exist in the DIN database");
            } else if (lowerBody.contains("\"status\":\"false\"") || lowerBody.contains("\"status\": \"false\"") || lowerBody.contains(MSG_NOT_MATCHING) || lowerBody.contains("not matching")) {
                return new DinPanVerifyResponse("not matched", din, pan, directorName,
                        "DIN details are not matching with the Income tax PAN database of MCA");
            } else if (lowerBody.contains(MSG_INVALID_DIN) || lowerBody.contains("invalid din")) {
                return new DinPanVerifyResponse("not matched", din, pan, directorName,
                        "Verification failed. Invalid DIN.");
            }

            String fallbackMsg = (dynamicStatus != null && !dynamicStatus.isEmpty()) ? dynamicStatus : responseBody;
            return new DinPanVerifyResponse("UNVERIFIED", din, pan, directorName, fallbackMsg);
        }
    }

    // Helper method to extract the director's name from the JSON response
    private String extractDirectorName(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode dataNode = root.path("data");
            if (!dataNode.isMissingNode()) {
                String first = dataNode.path("firstName").asText("");
                String middle = dataNode.path("middleName").asText("");
                String last = dataNode.path("lastName").asText("");
                if ("null".equalsIgnoreCase(first)) first = "";
                if ("null".equalsIgnoreCase(middle)) middle = "";
                if ("null".equalsIgnoreCase(last)) last = "";
                String fullName = (first + " " + middle + " " + last).replaceAll("\\s+", " ").trim();
                if (!fullName.isEmpty()) {
                    return fullName;
                }
                if (!dataNode.path("Name").isMissingNode()) {
                    return dataNode.path("Name").asText();
                }
                if (!dataNode.path("name").isMissingNode()) {
                    return dataNode.path("name").asText();
                }
            }
            JsonNode nameNode = root.path("Name");
            if (!nameNode.isMissingNode()) {
                return nameNode.asText();
            }
        } catch (Exception e) {
            // Ignore parsing errors for the name extraction
        }
        return null;
    }

    private String latestCookie(LoginResponse loginResponse, ValidateCaptchaResponse cResponse) {
        if (cResponse != null && cResponse.cookie() != null && !cResponse.cookie().isBlank()) {
            return cResponse.cookie();
        }
        return loginResponse.cookie();
    }

    public Map<String, Object> findDinPanUrl() {
        Map<String, Object> result = new HashMap<>();
        List<String> endpoints = new ArrayList<>();
        List<String> scriptMatches = new ArrayList<>();
        try {
            ValidateCaptchaResponse cap = captchaService.captchaValidatonWrapper("");
            String cookie = cap.status() ? cap.cookie() : "";
            Request pageReq = new Request.Builder()
                    .url("https://www.mca.gov.in/content/mca/global/en/mca/fo-llp-services/verify-din-pan-details-of-director.html")
                    .get()
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("accept-language", "en-US,en;q=0.9")
                    .header("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36")
                    .header("cookie", cookie)
                    .build();
            try (Response pageResp = http.execute(pageReq)) {
                String html = pageResp.body() == null ? "" : pageResp.body().string();
                result.put("pageStatus", pageResp.code());
                Set<String> jsUrls = new HashSet<>();
                Pattern jsPat = Pattern.compile("src=[\"']([^\"']+\\.js[^\"']*)[\"']");
                Matcher m = jsPat.matcher(html);
                while (m.find()) {
                    String url = m.group(1);
                    if (url.startsWith("/")) {
                        url = "https://www.mca.gov.in" + url;
                    }
                    jsUrls.add(url);
                }
                result.put("jsUrlsFound", jsUrls);

                Pattern binPat = Pattern.compile("/bin/mca/[A-Za-z0-9_.-]+");
                Matcher pageBin = binPat.matcher(html);
                while (pageBin.find()) {
                    endpoints.add("HTML: " + pageBin.group());
                }

                for (String jsUrl : jsUrls) {
                    if (jsUrl.contains("foservicesuc2") || jsUrl.contains("encrptdecrypt") || jsUrl.contains("clientlib-site")) {
                        try {
                            Request jsReq = new Request.Builder().url(jsUrl).get()
                                    .header("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36")
                                    .header("accept", "*/*")
                                    .header("accept-language", "en-US,en;q=0.9")
                                    .header("referer", "https://www.mca.gov.in/content/mca/global/en/mca/fo-llp-services/verify-din-pan-details-of-director.html")
                                    .header("sec-fetch-dest", "script")
                                    .header("sec-fetch-mode", "no-cors")
                                    .header("sec-fetch-site", "same-origin")
                                    .header("cookie", cookie)
                                    .build();
                            try (Response jsResp = http.execute(jsReq)) {
                                String jsCode = jsResp.body() == null ? "" : jsResp.body().string();
                                result.put(jsUrl, jsCode);
                            }
                        } catch (Exception ex) {
                            result.put(jsUrl + "_error", ex.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }
}
