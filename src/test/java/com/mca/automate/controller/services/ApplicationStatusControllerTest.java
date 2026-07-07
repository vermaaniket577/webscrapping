package com.mca.automate.controller.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mca.automate.dto.ApplicationHistoryRequestDTO;
import com.mca.automate.dto.DownloadStatusDocumentDTO;
import com.mca.automate.dto.LoginResponse;
import com.mca.automate.dto.MasterDataRequest;
import com.mca.automate.service.MCAApplicationStatusService;
import com.mca.automate.service.MCADownloadAnnualFilling;
import com.mca.automate.service.McaLoginService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

class ApplicationStatusControllerTest {

    @Mock
    private McaLoginService loginService;

    @Mock
    private MCAApplicationStatusService applicationStatusService;

    @Mock
    private MCADownloadAnnualFilling downloadAnnualFilling;

    private ApplicationStatusController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new ApplicationStatusController();
        controller.mcaLoginService = loginService;
        controller.applicationStatusService = applicationStatusService;
        controller.downloadAnnualFilling = downloadAnnualFilling;
    }

    @Test
    void suppliedVerifiedCookieFetchesApplicationStatusWithoutLoginOrLogout() throws Exception {
        ApplicationHistoryRequestDTO request = applicationHistoryRequest();
        when(applicationStatusService.getApplicationHistory(request, "app-cookie")).thenReturn("application-status-response");

        String response = controller.fectAppStatus(request, "app-cookie\n");

        assertThat(response).isEqualTo("application-status-response");
        verify(loginService, never()).login(any(), any(), any(), any());
        verify(loginService, never()).logout(any());
    }

    @Test
    void applicationStatusOtpRequiredReturnsCookieForOtpStep() throws Exception {
        ApplicationHistoryRequestDTO request = applicationHistoryRequest();
        when(loginService.login("user@example.com", "secret", "device-1", "login"))
                .thenReturn(new LoginResponse("app-otp-cookie", "Otp Required", true));

        String response = controller.fectAppStatus(request, null);

        assertThat(response).isEqualTo("app-otp-cookie");
        verify(applicationStatusService, never()).getApplicationHistory(any(), any());
    }

    @Test
    void suppliedVerifiedCookieDownloadsAllWithoutLoginOrLogout() throws Exception {
        DownloadStatusDocumentDTO request = downloadStatusRequest();
        ResponseEntity<Resource> expected = textResource("zip-bytes");
        when(applicationStatusService.docDownloadList(request, "download-cookie")).thenReturn(expected);

        ResponseEntity<Resource> response = controller.downloadAll(request, "download-cookie\n");

        assertThat(response).isSameAs(expected);
        verify(loginService, never()).login(any(), any(), any(), any());
        verify(loginService, never()).logout(any());
    }

    @Test
    void downloadAllOtpRequiredReturnsCookieBodyForOtpStep() throws Exception {
        DownloadStatusDocumentDTO request = downloadStatusRequest();
        when(loginService.login("user@example.com", "secret", "device-1", "login"))
                .thenReturn(new LoginResponse("download-otp-cookie", "Otp Required", true));

        ResponseEntity<Resource> response = controller.downloadAll(request, null);

        assertThat(resourceText(response.getBody())).isEqualTo("download-otp-cookie");
        verify(applicationStatusService, never()).docDownloadList(any(), any());
    }

    @Test
    void suppliedVerifiedCookieFetchesAnnualFilingDocsWithoutLoginOrLogout() throws Exception {
        MasterDataRequest request = masterDataRequest();
        ResponseEntity<Resource> expected = textResource("{\"annual\":true}");
        when(downloadAnnualFilling.fetchDocumentList(request, "annual-cookie")).thenReturn(expected);

        ResponseEntity<Resource> response = controller.FetchAnnualFilling(request, "annual-cookie\n");

        assertThat(response).isSameAs(expected);
        verify(loginService, never()).login(any(), any(), any(), any());
        verify(loginService, never()).logout(any());
    }

    @Test
    void fetchAnnualFilingOtpRequiredReturnsCookieBodyForOtpStep() throws Exception {
        MasterDataRequest request = masterDataRequest();
        when(loginService.login("user@example.com", "secret", "device-1", "login"))
                .thenReturn(new LoginResponse("annual-otp-cookie", "Otp Required", true));

        ResponseEntity<Resource> response = controller.FetchAnnualFilling(request, null);

        assertThat(resourceText(response.getBody())).isEqualTo("annual-otp-cookie");
        verify(downloadAnnualFilling, never()).fetchDocumentList(any(), any());
    }

    @Test
    void annualFilingDmsDownloadPassesCallerCookieWhenPresent() throws Exception {
        ResponseEntity<Resource> expected = textResource("pdf-bytes");
        when(downloadAnnualFilling.downloadDocuments(eq("68034251"), eq("doc-cookie"))).thenReturn(expected);

        ResponseEntity<Resource> response = controller.downloadannualFillingDoc("68034251", "doc-cookie\n");

        assertThat(response).isSameAs(expected);
    }

    private ApplicationHistoryRequestDTO applicationHistoryRequest() {
        return new ApplicationHistoryRequestDTO(
                "applicationHistory",
                "AR",
                "L74909DL2008PLC180850",
                "",
                "",
                "",
                "",
                "user@example.com",
                "secret",
                "device-1");
    }

    private DownloadStatusDocumentDTO downloadStatusRequest() {
        return new DownloadStatusDocumentDTO(
                "download",
                "REF123",
                "1",
                "SRN123",
                "",
                "user@example.com",
                "secret",
                "device-1");
    }

    private MasterDataRequest masterDataRequest() {
        MasterDataRequest request = new MasterDataRequest();
        request.setCompanyNameOrCIN("L74909DL2008PLC180850");
        request.setUserName("user@example.com");
        request.setPassword("secret");
        request.setDeviceId("device-1");
        return request;
    }

    private ResponseEntity<Resource> textResource(String text) {
        return ResponseEntity.ok(new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8)));
    }

    private String resourceText(Resource resource) throws Exception {
        assertThat(resource).isInstanceOf(ByteArrayResource.class);
        return new String(((ByteArrayResource) resource).getByteArray(), StandardCharsets.UTF_8);
    }
}
