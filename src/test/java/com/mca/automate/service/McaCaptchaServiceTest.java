package com.mca.automate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mca.automate.dto.FetchCaptchaResponse;
import com.mca.automate.util.CookieUtil;
import com.mca.automate.util.CryptoUtil;
import com.mca.automate.util.CustomCaptchaTextExtractor;
import com.mca.automate.util.ImageToTextExtractor;
import com.mca.automate.util.LocalVisionCaptchaTextExtractor;
import com.mca.automate.util.Preprocess;
import com.mca.automate.util.Util;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class McaCaptchaServiceTest {

    @TempDir
    Path auditDir;

    @Test
    void getCaptchaDoesNotStoreReceivedImageByDefault() throws Exception {
        HttpClientService http = mock(HttpClientService.class);
        LocalVisionCaptchaTextExtractor localVisionExtractor = mock(LocalVisionCaptchaTextExtractor.class);
        CustomCaptchaTextExtractor customExtractor = mock(CustomCaptchaTextExtractor.class);
        CookieUtil cookieUtil = mock(CookieUtil.class);
        Preprocess preProcess = mock(Preprocess.class);
        ImageToTextExtractor imageToTextExtractor = mock(ImageToTextExtractor.class);
        McaCaptchaService service = new McaCaptchaService(
                http,
                new Util(),
                preProcess,
                imageToTextExtractor,
                localVisionExtractor,
                customExtractor,
                mock(CryptoUtil.class));
        service.cookieUtil = cookieUtil;

        when(http.execute(any(Request.class))).thenReturn(captchaResponse());
        when(cookieUtil.updateCookies(any(), any(Response.class))).thenReturn("updated-cookie");

        String previousAuditDir = System.getProperty("mca.captcha.audit.dir");
        String previousAuditEnabled = System.getProperty("mca.captcha.audit.enabled");
        String previousExtractionEnabled = System.getProperty("mca.captcha.extraction.enabled");
        System.setProperty("mca.captcha.audit.dir", auditDir.toString());
        System.setProperty("mca.captcha.extraction.enabled", "false");
        try {
            FetchCaptchaResponse result = service.getCaptcha("initial-cookie", false);

            assertThat(result.status()).isFalse();
            assertThat(result.captcha()).isBlank();
            assertThat(Files.list(auditDir)
                    .filter(path -> path.getFileName().toString().matches("captcha_.*\\.png"))
                    .count()).isZero();
            verifyNoInteractions(preProcess, imageToTextExtractor, localVisionExtractor, customExtractor);
        }
        finally {
            restoreProperty("mca.captcha.audit.dir", previousAuditDir);
            restoreProperty("mca.captcha.audit.enabled", previousAuditEnabled);
            restoreProperty("mca.captcha.extraction.enabled", previousExtractionEnabled);
        }
    }

    @Test
    void getCaptchaStoresReceivedImageWhenAuditIsEnabled() throws Exception {
        HttpClientService http = mock(HttpClientService.class);
        LocalVisionCaptchaTextExtractor localVisionExtractor = mock(LocalVisionCaptchaTextExtractor.class);
        CustomCaptchaTextExtractor customExtractor = mock(CustomCaptchaTextExtractor.class);
        CookieUtil cookieUtil = mock(CookieUtil.class);
        Preprocess preProcess = mock(Preprocess.class);
        ImageToTextExtractor imageToTextExtractor = mock(ImageToTextExtractor.class);
        McaCaptchaService service = new McaCaptchaService(
                http,
                new Util(),
                preProcess,
                imageToTextExtractor,
                localVisionExtractor,
                customExtractor,
                mock(CryptoUtil.class));
        service.cookieUtil = cookieUtil;

        when(http.execute(any(Request.class))).thenReturn(captchaResponse());
        when(cookieUtil.updateCookies(any(), any(Response.class))).thenReturn("updated-cookie");

        String previousAuditDir = System.getProperty("mca.captcha.audit.dir");
        String previousAuditEnabled = System.getProperty("mca.captcha.audit.enabled");
        String previousExtractionEnabled = System.getProperty("mca.captcha.extraction.enabled");
        System.setProperty("mca.captcha.audit.dir", auditDir.toString());
        System.setProperty("mca.captcha.audit.enabled", "true");
        System.setProperty("mca.captcha.extraction.enabled", "false");
        try {
            FetchCaptchaResponse result = service.getCaptcha("initial-cookie", false);

            assertThat(result.status()).isFalse();
            assertThat(result.captcha()).isBlank();
            assertThat(Files.list(auditDir)
                    .filter(path -> path.getFileName().toString().matches("captcha_.*\\.png"))
                    .count()).isEqualTo(1);
            verifyNoInteractions(preProcess, imageToTextExtractor, localVisionExtractor, customExtractor);
        }
        finally {
            restoreProperty("mca.captcha.audit.dir", previousAuditDir);
            restoreProperty("mca.captcha.audit.enabled", previousAuditEnabled);
            restoreProperty("mca.captcha.extraction.enabled", previousExtractionEnabled);
        }
    }

    @Test
    void getCaptchaUsesCustomExtractorWhenOcrIsNotEnabled() throws Exception {
        HttpClientService http = mock(HttpClientService.class);
        LocalVisionCaptchaTextExtractor localVisionExtractor = mock(LocalVisionCaptchaTextExtractor.class);
        CustomCaptchaTextExtractor customExtractor = mock(CustomCaptchaTextExtractor.class);
        CookieUtil cookieUtil = mock(CookieUtil.class);
        Preprocess preProcess = mock(Preprocess.class);
        ImageToTextExtractor imageToTextExtractor = mock(ImageToTextExtractor.class);
        McaCaptchaService service = new McaCaptchaService(
                http,
                new Util(),
                preProcess,
                imageToTextExtractor,
                localVisionExtractor,
                customExtractor,
                mock(CryptoUtil.class));
        service.cookieUtil = cookieUtil;

        when(http.execute(any(Request.class))).thenReturn(captchaResponse());
        when(cookieUtil.updateCookies(any(), any(Response.class))).thenReturn("updated-cookie");
        when(customExtractor.extractText(any())).thenReturn(Map.of("Zx34Cv", 0.95));

        String previousAuditDir = System.getProperty("mca.captcha.audit.dir");
        String previousExtractionEnabled = System.getProperty("mca.captcha.extraction.enabled");
        String previousOcrEnabled = System.getProperty("mca.captcha.ocr.enabled");
        System.setProperty("mca.captcha.audit.dir", auditDir.toString());
        System.setProperty("mca.captcha.extraction.enabled", "true");
        System.setProperty("mca.captcha.ocr.enabled", "false");
        try {
            FetchCaptchaResponse result = service.getCaptcha("initial-cookie", false);

            assertThat(result.status()).isTrue();
            assertThat(result.captcha()).isEqualTo("Zx34Cv");
            verify(customExtractor, times(1)).extractText(any());
            verifyNoInteractions(preProcess, imageToTextExtractor, localVisionExtractor);
        }
        finally {
            restoreProperty("mca.captcha.audit.dir", previousAuditDir);
            restoreProperty("mca.captcha.extraction.enabled", previousExtractionEnabled);
            restoreProperty("mca.captcha.ocr.enabled", previousOcrEnabled);
        }
    }

    @Test
    void getCaptchaUsesOcrWhenExplicitlyEnabled() throws Exception {
        HttpClientService http = mock(HttpClientService.class);
        LocalVisionCaptchaTextExtractor localVisionExtractor = mock(LocalVisionCaptchaTextExtractor.class);
        CustomCaptchaTextExtractor customExtractor = mock(CustomCaptchaTextExtractor.class);
        CookieUtil cookieUtil = mock(CookieUtil.class);
        Preprocess preProcess = mock(Preprocess.class);
        ImageToTextExtractor imageToTextExtractor = mock(ImageToTextExtractor.class);
        McaCaptchaService service = new McaCaptchaService(
                http,
                new Util(),
                preProcess,
                imageToTextExtractor,
                localVisionExtractor,
                customExtractor,
                mock(CryptoUtil.class));
        service.cookieUtil = cookieUtil;

        when(http.execute(any(Request.class))).thenReturn(captchaResponse());
        when(cookieUtil.updateCookies(any(), any(Response.class))).thenReturn("updated-cookie");
        when(preProcess.preProcessCaptchaVariants(any())).thenReturn(List.of(new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB)));
        when(imageToTextExtractor.getCaptchaText(any())).thenReturn("Ab12Cd");

        String previousAuditDir = System.getProperty("mca.captcha.audit.dir");
        String previousExtractionEnabled = System.getProperty("mca.captcha.extraction.enabled");
        String previousOcrEnabled = System.getProperty("mca.captcha.ocr.enabled");
        System.setProperty("mca.captcha.audit.dir", auditDir.toString());
        System.setProperty("mca.captcha.extraction.enabled", "true");
        System.setProperty("mca.captcha.ocr.enabled", "true");
        try {
            FetchCaptchaResponse result = service.getCaptcha("initial-cookie", false);

            assertThat(result.status()).isTrue();
            assertThat(result.captcha()).isEqualTo("Ab12Cd");
            verify(preProcess, times(1)).preProcessCaptchaVariants(any());
            verify(imageToTextExtractor, times(1)).getCaptchaText(any());
            verifyNoInteractions(localVisionExtractor, customExtractor);
        }
        finally {
            restoreProperty("mca.captcha.audit.dir", previousAuditDir);
            restoreProperty("mca.captcha.extraction.enabled", previousExtractionEnabled);
            restoreProperty("mca.captcha.ocr.enabled", previousOcrEnabled);
        }
    }

    @Test
    void getCaptchaFallsBackToCustomExtractorWhenOcrCandidateIsInvalid() throws Exception {
        HttpClientService http = mock(HttpClientService.class);
        LocalVisionCaptchaTextExtractor localVisionExtractor = mock(LocalVisionCaptchaTextExtractor.class);
        CustomCaptchaTextExtractor customExtractor = mock(CustomCaptchaTextExtractor.class);
        CookieUtil cookieUtil = mock(CookieUtil.class);
        Preprocess preProcess = mock(Preprocess.class);
        ImageToTextExtractor imageToTextExtractor = mock(ImageToTextExtractor.class);
        McaCaptchaService service = new McaCaptchaService(
                http,
                new Util(),
                preProcess,
                imageToTextExtractor,
                localVisionExtractor,
                customExtractor,
                mock(CryptoUtil.class));
        service.cookieUtil = cookieUtil;

        when(http.execute(any(Request.class))).thenReturn(captchaResponse());
        when(cookieUtil.updateCookies(any(), any(Response.class))).thenReturn("updated-cookie");
        when(preProcess.preProcessCaptchaVariants(any())).thenReturn(List.of(new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB)));
        when(imageToTextExtractor.getCaptchaText(any())).thenReturn("12345");
        when(customExtractor.extractText(any())).thenReturn(Map.of("Zx34Cv", 0.95));

        String previousAuditDir = System.getProperty("mca.captcha.audit.dir");
        String previousExtractionEnabled = System.getProperty("mca.captcha.extraction.enabled");
        String previousOcrEnabled = System.getProperty("mca.captcha.ocr.enabled");
        System.setProperty("mca.captcha.audit.dir", auditDir.toString());
        System.setProperty("mca.captcha.extraction.enabled", "true");
        System.setProperty("mca.captcha.ocr.enabled", "true");
        try {
            FetchCaptchaResponse result = service.getCaptcha("initial-cookie", false);

            assertThat(result.status()).isTrue();
            assertThat(result.captcha()).isEqualTo("Zx34Cv");
            verify(preProcess, times(1)).preProcessCaptchaVariants(any());
            verify(imageToTextExtractor, times(1)).getCaptchaText(any());
            verify(customExtractor, times(1)).extractText(any());
            verifyNoInteractions(localVisionExtractor);
        }
        finally {
            restoreProperty("mca.captcha.audit.dir", previousAuditDir);
            restoreProperty("mca.captcha.extraction.enabled", previousExtractionEnabled);
            restoreProperty("mca.captcha.ocr.enabled", previousOcrEnabled);
        }
    }

    private void restoreProperty(String name, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, previousValue);
        }
    }

    private Response captchaResponse() throws Exception {
        byte[] png = pngBytes();
        return new Response.Builder()
                .request(new Request.Builder().url("https://www.mca.gov.in/bin/mca/generateCaptchaWithHMAC").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("Content-Type", "image/png")
                .header("pre_ct", "pre-ct-value")
                .body(ResponseBody.create(png, MediaType.parse("image/png")))
                .build();
    }

    private byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
