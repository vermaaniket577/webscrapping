package com.mca.automate.service;

import com.mca.automate.dto.FetchCaptchaResponse;
import com.mca.automate.dto.ValidateCaptchaRequest;
import com.mca.automate.dto.ValidateCaptchaResponse;
import com.mca.automate.service.HttpClientService;
import com.mca.automate.util.CookieUtil;
import com.mca.automate.util.CryptoUtil;
import com.mca.automate.util.CustomCaptchaTextExtractor;
import com.mca.automate.util.ImageToTextExtractor;
import com.mca.automate.util.MimePart;
import com.mca.automate.util.Preprocess;
import com.mca.automate.util.Util;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import lombok.Generated;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class McaCaptchaService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(McaCaptchaService.class);
    private static final Pattern CAPTCHA_PATTERN = Pattern.compile("^[A-Za-z0-9]{6,8}$");
    private final HttpClientService http;
    private static final String CAPTCHA_URL = "https://www.mca.gov.in/bin/mca/generateCaptchaWithHMAC";
    private static final String CSRF_TOKEN_URL = "https://www.mca.gov.in/libs/granite/csrf/token.json";
    private static final MediaType FORM = MediaType.parse((String)"application/x-www-form-urlencoded; charset=UTF-8");
    private static final String CAPTCHAVALIDATION_URL = "https://www.mca.gov.in/bin/mca/HmacCaptchaValidationServlet";
    private final Util util;
    private final Preprocess preProcess;
    private final ImageToTextExtractor imageToTextExtractor;
    private final CustomCaptchaTextExtractor customCaptchaTextExtractor;
    private final CryptoUtil cryptoUtil;
    @Autowired
    CookieUtil cookieUtil;

    public ValidateCaptchaResponse captchaValidatonWrapper(String cookie) throws Exception {
        log.info("=== Starting Captcha Wrapper ===");
        int maxRetry = 3;
        ValidateCaptchaResponse response = null;
        for (int attempt = 1; attempt <= maxRetry; ++attempt) {
            log.info(" Attempt {}/{}", (Object)attempt, (Object)maxRetry);
            FetchCaptchaResponse captcha = this.getCaptcha(cookie, true);
            if (!captcha.status() || captcha.captcha() == null || captcha.captcha().isBlank()) {
                log.warn(attempt < maxRetry ? "Captcha fetch/extraction failed. Retrying..." : "Captcha fetch/extraction failed. No attempts left.");
                if (attempt < maxRetry) {
                    this.sleep(attempt);
                }
                continue;
            }
            ValidateCaptchaRequest req = new ValidateCaptchaRequest(captcha.captcha(), captcha.preCt(), captcha.status(), cookie);
            // Prime CSRF after captcha generation because MCA can rotate cookies during the captcha call.
            req = new ValidateCaptchaRequest(req.captchaTxt(), req.preCt(), req.status(), this.primeCsrfToken(req.cookie()));
            response = this.validateCaptcha(req);
            if (response != null && response.status()) {
                log.info("Captcha validated successfully");
                return response;
            }
            log.warn(attempt < maxRetry ? "Captcha validation failed. Retrying..." : "Captcha validation failed. No attempts left.");
            if (attempt < maxRetry) {
                this.sleep(attempt);
            }
        }
        log.error("Captcha failed after {} attempts", (Object)maxRetry);
        return new ValidateCaptchaResponse("", "", cookie, false);
    }

    public FetchCaptchaResponse getCaptcha(String cookie) throws Exception {
        return this.getCaptcha(cookie, true);
    }

    public FetchCaptchaResponse getCaptcha(String cookie, boolean forceCustomExtractor) throws Exception {
        log.info("Fetching captcha from URL: {}", (Object)CAPTCHA_URL);
        try (Response response = this.http.execute(new Request.Builder().url(CAPTCHA_URL).get().header("cookie", cookie).header("accept", "*/*").header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8").header("origin", "https://www.mca.gov.in").header("referer", "https://www.mca.gov.in/content/mca/global/en/foportal/fologin.html").header("sec-fetch-dest", "empty").header("sec-fetch-mode", "cors").header("sec-fetch-site", "same-origin").header("x-requested-with", "XMLHttpRequest").header("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").build())) {
            cookie = this.cookieUtil.updateCookies(cookie, response);
            if (response.body() == null) {
                log.error("Captcha response NULL");
                return new FetchCaptchaResponse("", "", false, cookie);
            }
            log.info("Captcha Response Status Code: {}", (Object)response.code());
            String ct = Optional.ofNullable(response.header("Content-Type")).orElse("");
            log.info("Captcha response content-type: {}", (Object)ct);
            log.info("Captcha response headers: {}", response.headers());
            byte[] raw = response.body().bytes();
            log.info("Captcha response payload bytes: {}", (Object)raw.length);
            if (raw.length == 0) {
                log.error("Empty captcha response");
                return new FetchCaptchaResponse("", "", false, cookie);
            }
            byte[] image = this.extractCaptchaImage(raw, ct);
            if (image == null) {
                log.warn("Captcha image not found in response with Content-Type {}", (Object)ct);
                return new FetchCaptchaResponse("", "", false, cookie);
            }
            log.info("Captcha image bytes extracted: {}", (Object)image.length);
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            String captchaText = this.readCaptchaWithFallback(image, timestamp, forceCustomExtractor);
            if (!this.isPlausibleCaptcha(captchaText)) {
                log.warn("OCR produced invalid captcha candidate: {}", (Object)captchaText);
                return new FetchCaptchaResponse("", "", false, cookie);
            }
            String preCt = this.util.stripNewlines(this.util.safe(response.header("pre_ct")));
            if (preCt.isEmpty()) {
                log.warn("Captcha response missing pre_ct header");
                return new FetchCaptchaResponse("", "", false, cookie);
            }
            return new FetchCaptchaResponse(captchaText, preCt, true, cookie);
        }
        catch (Exception ex) {
            log.error("Failed to fetch captcha from URL {} -- MCA BLOCKED? ERROR: {}", (Object)CAPTCHA_URL, (Object)ex.getMessage(), (Object)ex);
            return new FetchCaptchaResponse("", "", false, "");
        }
    }

    public ValidateCaptchaResponse validateCaptcha(ValidateCaptchaRequest validateCaptchaRequest) throws IOException {
        String cookie = validateCaptchaRequest.cookie();
        String captchaText = this.util.stripNewlines(this.util.safe(validateCaptchaRequest.captchaTxt()));
        String preCt = this.util.stripNewlines(this.util.safe(validateCaptchaRequest.preCt()));
        String dataForCaptcha = "userInput=" + captchaText + "&pre_CT=" + preCt;
        log.info("Validate captcha plaintext payload before encryption: {}", (Object)dataForCaptcha);
        log.info("Validate captcha request metadata: captcha='{}', captchaLength={}, preCtLength={}, preCtPreview={}, cookiePresent={}", captchaText, captchaText.length(), preCt.length(), this.abbreviate(preCt), cookie != null && !cookie.isBlank());
        String encryptedData = this.cryptoUtil.encrypt(dataForCaptcha);
        String bodyStr = "data=" + encryptedData;
        log.info("Validate captcha form keys: data");
        log.info("Validate captcha encrypted body length: {}", (Object)bodyStr.length());
        RequestBody body = RequestBody.create((String)bodyStr, (MediaType)FORM);
        Request request = new Request.Builder().url(CAPTCHAVALIDATION_URL).post(body).header("cookie", cookie).header("accept", "*/*").header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8").header("origin", "https://www.mca.gov.in").header("referer", "https://www.mca.gov.in/content/mca/global/en/foportal/fologin.html").header("sec-fetch-dest", "empty").header("sec-fetch-mode", "cors").header("sec-fetch-site", "same-origin").header("x-requested-with", "XMLHttpRequest").header("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36").build();
        try (Response response = this.http.execute(request)) {
            cookie = this.cookieUtil.updateCookies(cookie, response);
            log.info("Validate captcha response code is {}", (Object)response.code());
            log.info("Validate captcha response headers: {}", response.headers());
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!responseBody.isBlank()) {
                log.info("Validate captcha response body: {}", (Object)this.printable(responseBody));
            }
            String preCtHeader = response.header("pre_CT");
            boolean bodySuccess = response.code() == 200 && responseBody.contains("\"success\": true");
            if (!bodySuccess) {
                log.warn("Validate captcha did not return success=true");
                return new ValidateCaptchaResponse("", "", cookie, false);
            }
            if (null == preCtHeader || preCtHeader.isBlank()) {
                log.warn("Validate captcha response did not include pre_CT header; keeping captcha pre_CT from generated response");
                preCtHeader = preCt;
            }
            return new ValidateCaptchaResponse(validateCaptchaRequest.captchaTxt(), this.util.stripNewlines(this.util.safe(preCtHeader)), cookie, true);
        }
    }

    private String primeCsrfToken(String cookie) {
        Request request = new Request.Builder()
                .url(CSRF_TOKEN_URL)
                .get()
                .header("cookie", cookie)
                .header("accept", "application/json, text/javascript, */*; q=0.01")
                .header("accept-language", "en-GB,en-US;q=0.9,en;q=0.8")
                .header("referer", "https://www.mca.gov.in/content/mca/global/en/foportal/fologin.html")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-origin")
                .header("x-requested-with", "XMLHttpRequest")
                .header("user-agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36")
                .build();
        try (Response response = this.http.execute(request)) {
            String updatedCookie = this.cookieUtil.updateCookies(cookie, response);
            log.info("CSRF token priming response code is {}", (Object)response.code());
            String responseBody = response.body() == null ? "" : response.body().string();
            if (!responseBody.isBlank()) {
                log.info("CSRF token priming response body: {}", (Object)this.printable(responseBody));
            }
            return updatedCookie;
        }
        catch (Exception ex) {
            log.warn("CSRF token priming failed; continuing with existing cookies", (Throwable)ex);
            return cookie;
        }
    }

    private byte[] extractCaptchaImage(byte[] raw, String contentType) throws IOException {
        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        log.info("extractCaptchaImage called with content-type: {}", (Object)normalizedContentType);
        if (normalizedContentType.startsWith("image/")) {
            log.info("Captcha response is a direct image payload");
            return raw;
        }
        if (!normalizedContentType.startsWith("multipart/")) {
            log.warn("Captcha response is not image or multipart, skipping");
            return null;
        }
        List<MimePart> parts = this.util.parseMultipart(raw, contentType);
        log.info("Multipart captcha response parsed into {} parts", (Object)parts.size());
        for (int i = 0; i < parts.size(); ++i) {
            MimePart part = parts.get(i);
            log.info("Multipart part {} headers: {}", (Object)i, (Object)part.headers);
            log.info("Multipart part {} size: {}", (Object)i, (Object)part.body.length);
        }
        for (MimePart part : parts) {
            String partContentType = part.headers.getOrDefault("content-type", "").toLowerCase(Locale.ROOT);
            log.info("Inspecting multipart part content-type: {}", (Object)partContentType);
            if (partContentType.startsWith("image/")) {
                log.info("Found captcha image part with content-type {}", (Object)partContentType);
                return part.body;
            }
        }
        log.warn("No multipart image part found in captcha response");
        return null;
    }

    private String readCaptchaWithVariants(byte[] image, String timestamp) throws IOException {
        List<BufferedImage> variants = this.preProcess.preProcessCaptchaVariants(image);
        String bestCandidate = "";
        List<String> guesses = new ArrayList<>();
        for (int i = 0; i < variants.size(); ++i) {
            BufferedImage variant = variants.get(i);
            try {
                String rawText = this.imageToTextExtractor.getCaptchaText(variant);
                String candidate = this.normalizeCaptchaText(rawText);
                boolean plausible = this.isPlausibleCaptcha(candidate);
                guesses.add("variant=" + i + ", raw='" + this.printable(rawText) + "', normalized='" + candidate + "', plausible=" + plausible);
                log.info("OCR variant {} produced candidate '{}'", (Object)i, (Object)candidate);
                if (plausible) {
                    return candidate;
                }
                if (candidate.length() > bestCandidate.length()) {
                    bestCandidate = candidate;
                }
            }
            catch (Exception e) {
                guesses.add("variant=" + i + ", error='" + this.printable(e.getMessage()) + "'");
                log.warn("OCR failed for captcha variant {}", (Object)i, (Object)e);
            }
        }
        return bestCandidate;
    }

    private String readCaptchaWithFallback(byte[] image, String timestamp, boolean forceCustomExtractor) throws IOException {
        String candidate = "";
        if (!forceCustomExtractor) {
            candidate = this.readCaptchaWithVariants(image, timestamp);
        }
        if (!forceCustomExtractor && this.isPlausibleCaptcha(candidate)) {
            return candidate;
        }
        log.info("Using custom captcha extractor hook{}", forceCustomExtractor ? " after first OCR attempt" : " because primary OCR flow failed");
        // The custom extractor works with an image path, so save a short-lived temp PNG.
        String imagePath = this.saveCaptchaPngForFallback(image, timestamp);
        if (imagePath.isBlank()) {
            return candidate;
        }
        try {
            List<Map.Entry<String, Double>> sortedGuesses = this.customCaptchaTextExtractor.extractText(imagePath).entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .toList();

            for (Map.Entry<String, Double> guess : sortedGuesses) {
                log.info("Custom extractor guess '{}' probability {}", guess.getKey(), guess.getValue());
            }

            String customCandidate = sortedGuesses.isEmpty()
                    ? ""
                    : this.normalizeCaptchaText(sortedGuesses.get(0).getKey());
            if (!customCandidate.isBlank()) {
                log.info("Custom captcha extractor produced candidate '{}'", (Object)customCandidate);
                return customCandidate;
            }
        }
        catch (Exception ex) {
            log.warn("Custom captcha extractor failed", (Throwable)ex);
        }
        return candidate;
    }

    private String normalizeCaptchaText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[^A-Za-z0-9]", "");
    }

    private boolean isPlausibleCaptcha(String captchaText) {
        return captchaText != null && CAPTCHA_PATTERN.matcher(captchaText).matches();
    }

    private String saveCaptchaPngForFallback(byte[] imageBytes, String timestamp) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                log.warn("Could not decode captcha image for fallback PNG output");
                return "";
            }
            Path tempFile = Files.createTempFile("mca-captcha-" + timestamp + "-", ".png");
            File file = tempFile.toFile();
            file.deleteOnExit();
            ImageIO.write(image, "png", file);
            return file.getAbsolutePath();
        }
        catch (Exception e) {
            log.error("Failed to save fallback captcha image", e);
            return "";
        }
    }

    private String printable(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r", "\\r").replace("\n", "\\n");
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= 16) {
            return value;
        }
        return value.substring(0, 8) + "..." + value.substring(value.length() - 8);
    }

    private void sleep(int attempt) {
        try {
            Thread.sleep(1000L * (long)attempt);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Generated
    public McaCaptchaService(HttpClientService http, Util util, Preprocess preProcess, ImageToTextExtractor imageToTextExtractor, CustomCaptchaTextExtractor customCaptchaTextExtractor, CryptoUtil cryptoUtil) {
        this.http = http;
        this.util = util;
        this.preProcess = preProcess;
        this.imageToTextExtractor = imageToTextExtractor;
        this.customCaptchaTextExtractor = customCaptchaTextExtractor;
        this.cryptoUtil = cryptoUtil;
    }
}
