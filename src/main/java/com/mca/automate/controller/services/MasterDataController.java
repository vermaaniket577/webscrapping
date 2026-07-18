package com.mca.automate.controller.services;

import com.mca.automate.dto.DinPanVerifyRequest;
import com.mca.automate.dto.DinPanVerifyResponse;
import com.mca.automate.dto.DirectorDataRequest;
import com.mca.automate.dto.LoginResponse;
import com.mca.automate.dto.MasterDataRequest;
import com.mca.automate.dto.ValidateCaptchaResponse;
import com.mca.automate.service.CookieService;
import com.mca.automate.service.McaCaptchaService;
import com.mca.automate.service.McaLoginService;
import com.mca.automate.service.McaSearchService;
import com.mca.automate.util.CryptoUtil;
import com.mca.automate.util.Util;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/* JADX INFO: loaded from: MasterDataController.class */
@RestController
public class MasterDataController {

    @Autowired
    McaCaptchaService captchaService;

    @Autowired
    McaLoginService mcaLoginService;

    @Autowired
    McaSearchService mcaSearchService;

    @Autowired
    CryptoUtil cryptoUtil;

    @Autowired
    Util util;

    @Autowired
    CookieService cookieService;

    private ResponseEntity<?> formatResponse(String responseData) {
        if (responseData == null) return ResponseEntity.ok().build();
        if (responseData.startsWith("Missing request parameter") || responseData.startsWith("Login Failed")
                || responseData.startsWith("Captcha validation failed")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", responseData));
        }
        if (responseData.startsWith("{\"error\":\"MCA")) {
            // Upstream MCA refused or failed the call; keep the compact JSON body but flag it as a
            // gateway error instead of masking it behind 200 OK.
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).contentType(MediaType.APPLICATION_JSON)
                    .body(responseData);
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(responseData);
    }

    @PostMapping(value = { "/getcompanymasterdata" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> fetchMasterData(@RequestBody MasterDataRequest masterDataRequest,
            @RequestHeader(value = "Cookie", required = false) String sessionCookie) throws Exception {
        String incomingCookie = this.normalizeIncomingCookie(sessionCookie);
        if (!incomingCookie.isBlank()) {
            System.out.println("Using caller supplied MCA session cookie for getcompanymasterdata");
            return formatResponse(this.fetchMasterDataWithAuthenticatedCookie(masterDataRequest, incomingCookie, false));
        }
        LoginResponse loginResponse = this.mcaLoginService.login(masterDataRequest.getUserName(),
                masterDataRequest.getPassword(), masterDataRequest.getDeviceId(), "login");
        System.out.println("Login Response is " + loginResponse.status() + " cookie is " + loginResponse.cookie());
        if (false == loginResponse.status()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", loginResponse.message()));
        }
        if (loginResponse.status() && "Otp Required".equalsIgnoreCase(loginResponse.message())) {
            return ResponseEntity.ok(Map.of("message", "Otp Required", "cookie", loginResponse.cookie()));
        }
        return formatResponse(this.fetchMasterDataWithAuthenticatedCookie(masterDataRequest, loginResponse.cookie(), true));
    }

    private String fetchMasterDataWithAuthenticatedCookie(MasterDataRequest masterDataRequest, String cookie,
            boolean logoutWhenDone) throws Exception {
        LoginResponse loginResponse = new LoginResponse(cookie, "Authenticated Cookie", true);
        // MCA requires a fresh captcha for the search/master-data servlet even after
        // login.
        ValidateCaptchaResponse afterLoginCResponse = this.captchaService
                .captchaValidatonWrapper(loginResponse.cookie());
        if (!afterLoginCResponse.status()) {
            return "Missing request parameter!!!";
        }
        LoginResponse captchaCookieLoginResponse = new LoginResponse(afterLoginCResponse.cookie(),
                loginResponse.message(), loginResponse.status());
        System.out.println(afterLoginCResponse.captcha() + "===" + afterLoginCResponse.preCt());
        String fSearchResponseJson = this.mcaSearchService.search(masterDataRequest, captchaCookieLoginResponse,
                afterLoginCResponse);
        System.out.println("Search Data " + fSearchResponseJson);
        String responseData = this.mcaSearchService.searchMasterData(masterDataRequest.getCompanyNameOrCIN(),
                captchaCookieLoginResponse, fSearchResponseJson, "cin");
        if (logoutWhenDone) {
            // Do not logout caller-supplied cookies; those are controlled by the OTP/manual
            // client flow.
            this.mcaLoginService.logout(captchaCookieLoginResponse.cookie());
        }
        return responseData;
    }

    private String normalizeIncomingCookie(String cookie) {
        String normalized = this.util.stripNewlines(this.util.safe(cookie));
        if (normalized.isBlank()) {
            return "";
        }
        // Only a fully authenticated MCA jar may skip login. _csrf/__UUID-HASH exist on every
        // anonymous visit (the captcha bootstrap sets them), so they prove nothing. The cookies
        // MCA's protected servlets actually authenticate with are sessionID + session-token-md5 -
        // the same pair login/verifiedOTPLogin refuse to report success without.
        if (!hasCookie(normalized, "sessionID") || !hasCookie(normalized, "session-token-md5")) {
            System.out.println("Ignoring caller cookie without sessionID/session-token-md5; falling back to login");
            return "";
        }
        return normalized;
    }

    // True when the cookie string contains a non-blank value for the given cookie name.
    private boolean hasCookie(String cookie, String name) {
        if (cookie == null || cookie.isBlank()) {
            return false;
        }
        for (String part : cookie.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && name.equals(kv[0].trim()) && !kv[1].trim().isBlank()) {
                return true;
            }
        }
        return false;
    }

    @PostMapping(value = { "/getcompanymasterdataWithName" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> fetchMasterDataWithName(@RequestBody MasterDataRequest masterDataRequest,
            @RequestHeader(value = "Cookie", required = false) String sessionCookie) throws Exception {
        String incomingCookie = this.normalizeIncomingCookie(sessionCookie);
        if (!incomingCookie.isBlank()) {
            System.out.println("Using caller supplied MCA session cookie for getcompanymasterdataWithName");
            return formatResponse(this.fetchMasterDataWithNameAuthenticatedCookie(masterDataRequest, incomingCookie, false));
        }
        LoginResponse loginResponse = this.mcaLoginService.login(masterDataRequest.getUserName(),
                masterDataRequest.getPassword(), masterDataRequest.getDeviceId(), "login");
        System.out.println("Login Response is " + loginResponse.status() + " cookie is " + loginResponse.cookie());
        if (false == loginResponse.status()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", loginResponse.message()));
        }
        if (loginResponse.status() && "Otp Required".equalsIgnoreCase(loginResponse.message())) {
            return ResponseEntity.ok(Map.of("message", "Otp Required", "cookie", loginResponse.cookie()));
        }
        return formatResponse(this.fetchMasterDataWithNameAuthenticatedCookie(masterDataRequest, loginResponse.cookie(), true));
    }

    private String fetchMasterDataWithNameAuthenticatedCookie(MasterDataRequest masterDataRequest, String cookie,
            boolean logoutWhenDone) throws Exception {
        LoginResponse loginResponse = new LoginResponse(cookie, "Authenticated Cookie", true);
        // Name search also needs a captcha token, but should not force a second login
        // after OTP.
        ValidateCaptchaResponse afterLoginCResponse = this.captchaService
                .captchaValidatonWrapper(loginResponse.cookie());
        if (!afterLoginCResponse.status()) {
            return "Missing request parameter!!!";
        }
        LoginResponse captchaCookieLoginResponse = new LoginResponse(afterLoginCResponse.cookie(),
                loginResponse.message(), loginResponse.status());
        System.out.println(afterLoginCResponse.captcha() + "===" + afterLoginCResponse.preCt());
        String responseData = this.mcaSearchService.search(masterDataRequest, captchaCookieLoginResponse,
                afterLoginCResponse);
        if (logoutWhenDone) {
            // Do not logout caller-supplied cookies; those are controlled by the OTP/manual
            // client flow.
            this.mcaLoginService.logout(captchaCookieLoginResponse.cookie());
        }
        return responseData;
    }

    @PostMapping(value = { "/getdirectordata" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> fetchDirData(@RequestBody DirectorDataRequest directorDataRequest,
            @RequestHeader(value = "Cookie", required = false) String sessionCookie) throws Exception {
        System.out.println("inside Din Data+++++++");
        String incomingCookie = this.normalizeIncomingCookie(sessionCookie);
        if (!incomingCookie.isBlank()) {
            System.out.println("Using caller supplied MCA session cookie for getdirectordata");
            return formatResponse(this.fetchDirectorDataWithAuthenticatedCookie(directorDataRequest, incomingCookie, false));
        }
        LoginResponse loginResponse = this.mcaLoginService.login(directorDataRequest.getUserName(),
                directorDataRequest.getPassword(), directorDataRequest.getDeviceId(), "login");
        System.out.println("Login Response is " + loginResponse.status() + " cookie is " + loginResponse.cookie());
        if (false == loginResponse.status()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", loginResponse.message()));
        }
        if (loginResponse.status() && "Otp Required".equalsIgnoreCase(loginResponse.message())) {
            return ResponseEntity.ok(Map.of("message", "Otp Required", "cookie", loginResponse.cookie()));
        }
        return formatResponse(this.fetchDirectorDataWithAuthenticatedCookie(directorDataRequest, loginResponse.cookie(), true));
    }

    private String fetchDirectorDataWithAuthenticatedCookie(DirectorDataRequest directorDataRequest, String cookie,
            boolean logoutWhenDone) throws Exception {
        LoginResponse loginResponse = new LoginResponse(cookie, "Authenticated Cookie", true);
        String responseData = this.mcaSearchService.searchMasterData(directorDataRequest.getUserName(), loginResponse,
                directorDataRequest.getDin(), "din");
        if (logoutWhenDone) {
            this.mcaLoginService.logout(loginResponse.cookie());
        }
        return responseData;
    }

    @PostMapping(value = { "/checkcompanyname" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> checkCompayName(@RequestBody MasterDataRequest masterDataRequest,
            @RequestHeader(value = "Cookie", required = false) String sessionCookie) throws Exception {
        String incomingCookie = this.normalizeIncomingCookie(sessionCookie);
        if (!incomingCookie.isBlank()) {
            System.out.println("Using caller supplied MCA session cookie for checkcompanyname");
            return formatResponse(this.checkCompanyNameWithAuthenticatedCookie(masterDataRequest, incomingCookie, false));
        }
        LoginResponse loginResponse = this.mcaLoginService.login(masterDataRequest.getUserName(),
                masterDataRequest.getPassword(), masterDataRequest.getDeviceId(), "login");
        System.out.println("Login Response is " + loginResponse.status() + " cookie is " + loginResponse.cookie());
        if (false == loginResponse.status()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", loginResponse.message()));
        }
        if (loginResponse.status() && "Otp Required".equalsIgnoreCase(loginResponse.message())) {
            return ResponseEntity.ok(Map.of("message", "Otp Required", "cookie", loginResponse.cookie()));
        }
        return formatResponse(this.checkCompanyNameWithAuthenticatedCookie(masterDataRequest, loginResponse.cookie(), true));
    }

    private String checkCompanyNameWithAuthenticatedCookie(MasterDataRequest masterDataRequest, String cookie,
            boolean logoutWhenDone) throws Exception {
        LoginResponse loginResponse = new LoginResponse(cookie, "Authenticated Cookie", true);
        ValidateCaptchaResponse afterLoginCResponse = this.captchaService
                .captchaValidatonWrapper(loginResponse.cookie());
        if (!afterLoginCResponse.status()) {
            return "Missing request parameter!!!";
        }
        LoginResponse captchaCookieLoginResponse = new LoginResponse(afterLoginCResponse.cookie(),
                loginResponse.message(), loginResponse.status());
        System.out.println(afterLoginCResponse.captcha() + "===" + afterLoginCResponse.preCt());
        String responseData = this.mcaSearchService.checkCompanyName(masterDataRequest, captchaCookieLoginResponse,
                afterLoginCResponse);
        if (logoutWhenDone) {
            this.mcaLoginService.logout(captchaCookieLoginResponse.cookie());
        }
        return responseData;
    }

    @GetMapping(value = { "/dinstatus/{din}" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> dinstatus(@PathVariable String din) throws Exception {
        return formatResponse(this.mcaSearchService.checkDinStatus(din));
    }

    /**
     * Mirrors mca.gov.in's "Verify DIN/DPIN-PAN Details of Director" page: given a
     * DIN and a PAN,
     * looks up the director's name for the DIN and reports whether MCA considers
     * the PAN a match.
     * response.status() is one of VERIFIED, UNVERIFIED, PAN_NOT_FOUND, INVALID_DIN,
     * ERROR.
     * No MCA login is required - this is a public MCA service, same as
     * /dinstatus/{din}.
     */
    @PostMapping({ "/verifydinpan", "/verifyDinPan" })
    public ResponseEntity<DinPanVerifyResponse> verifyDinPan(@RequestBody DinPanVerifyRequest request) {
        try {
            if (request.getDin() == null || request.getPan() == null) {
                return ResponseEntity.badRequest().body(new DinPanVerifyResponse("ERROR", request.getDin(),
                        request.getPan(), null, "DIN and PAN are both required"));
            }

            DinPanVerifyResponse response = this.mcaSearchService.verifyDinPan(request.getDin(), request.getPan());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(new DinPanVerifyResponse("ERROR", request.getDin(), request.getPan(), null,
                    "Verification failed. Exception: " + e.getMessage()));
        }
    }

    @GetMapping({ "/verifydinpan", "/verifyDinPan" })
    public ResponseEntity<DinPanVerifyResponse> verifyDinPanGet(
            @org.springframework.web.bind.annotation.RequestParam("din") String din,
            @org.springframework.web.bind.annotation.RequestParam("pan") String pan) {
        try {
            if (din == null || pan == null) {
                return ResponseEntity.badRequest().body(new DinPanVerifyResponse("ERROR", din,
                        pan, null, "DIN and PAN are both required"));
            }

            DinPanVerifyResponse response = this.mcaSearchService.verifyDinPan(din, pan);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(new DinPanVerifyResponse("ERROR", din, pan, null,
                    "Verification failed. Exception: " + e.getMessage()));
        }
    }

    @GetMapping({ "/findDinPanUrl" })
    public Map<String, Object> findDinPanUrl() {
        return this.mcaSearchService.findDinPanUrl();
    }

    // ---- Directors / Designated partners: search by DIN/DPIN ----

    @PostMapping({ "/getdirectormasterdata" })
public String fetchDirectorMasterData(@RequestBody DirectorDataRequest directorDataRequest,
        @RequestHeader(value = "Cookie", required = false) String sessionCookie) throws Exception {
    String incomingCookie = this.normalizeIncomingCookie(sessionCookie);
    if (!incomingCookie.isBlank()) {
        // OTP verification returns an authenticated MCA cookie; reuse it to skip login.
        System.out.println("Using caller supplied MCA session cookie for getdirectormasterdata");
        return this.fetchDirectorMasterDataWithAuthenticatedCookie(directorDataRequest, incomingCookie, false);
    }
    LoginResponse loginResponse = this.mcaLoginService.login(directorDataRequest.getUserName(),
            directorDataRequest.getPassword(), directorDataRequest.getDeviceId(), "login");
    System.out.println("Login Response is " + loginResponse.status() + " cookie is " + loginResponse.cookie());
    if (false == loginResponse.status()) {
        return loginResponse.message();
    }
    if (loginResponse.status() && "Otp Required".equalsIgnoreCase(loginResponse.message())) {
        // Login split: client verifies OTP, then calls back with the verified cookie.
        return loginResponse.cookie();
    }
    return this.fetchDirectorMasterDataWithAuthenticatedCookie(directorDataRequest, loginResponse.cookie(), true);
}

private String fetchDirectorMasterDataWithAuthenticatedCookie(DirectorDataRequest directorDataRequest,
        String cookie, boolean logoutWhenDone) throws Exception {
    LoginResponse loginResponse = new LoginResponse(cookie, "Authenticated Cookie", true);
    // MCA requires a fresh captcha for the master-data servlet even after login.
    ValidateCaptchaResponse afterLoginCResponse = this.captchaService
            .captchaValidatonWrapper(loginResponse.cookie());
    if (!afterLoginCResponse.status()) {
        return "Missing request parameter!!!";
    }
    LoginResponse captchaCookieLoginResponse = new LoginResponse(afterLoginCResponse.cookie(),
            loginResponse.message(), loginResponse.status());
    System.out.println(afterLoginCResponse.captcha() + "===" + afterLoginCResponse.preCt());

    // Best-effort autosuggest (mirrors the "Directors/Designated partners" radio).
    // Its result is NOT used for the fetch below, so a wrong mdsSearchType won't break anything.
    try {
        String dSearchResponseJson = this.mcaSearchService.searchDirector(directorDataRequest.getDin(),
                captchaCookieLoginResponse, afterLoginCResponse);
        System.out.println("Director Search Data " + dSearchResponseJson);
    } catch (Exception e) {
        System.out.println("Director autosuggest skipped: " + e.getMessage());
    }

    // Master-data fetch by DIN (requestID=din handled inside searchMasterData).
    String responseData = this.mcaSearchService.searchMasterData(directorDataRequest.getDin(),
            captchaCookieLoginResponse, directorDataRequest.getDin(), "din");
    if (logoutWhenDone) {
        // Don't logout caller-supplied cookies; those are owned by the OTP/manual client flow.
        this.mcaLoginService.logout(captchaCookieLoginResponse.cookie());
    }
    return responseData;
}


}

