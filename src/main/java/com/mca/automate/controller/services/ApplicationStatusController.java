package com.mca.automate.controller.services;

import com.mca.automate.dto.ApplicationHistoryRequestDTO;
import com.mca.automate.dto.DownloadStatusDocumentDTO;
import com.mca.automate.dto.EditSRNRequestDto;
import com.mca.automate.dto.LoginResponse;
import com.mca.automate.dto.MasterDataRequest;
import com.mca.automate.service.MCAApplicationStatusService;
import com.mca.automate.service.MCADownloadAnnualFilling;
import com.mca.automate.service.McaLoginService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/* JADX INFO: loaded from: ApplicationStatusController.class */
@RestController
public class ApplicationStatusController {

    @Autowired
    McaLoginService mcaLoginService;

    @Autowired
    MCAApplicationStatusService applicationStatusService;

    @Autowired
    MCADownloadAnnualFilling downloadAnnualFilling;

    @PostMapping({"/applicationstatus"})
    public String fectAppStatus(@RequestBody ApplicationHistoryRequestDTO applicationHistoryRequestDTO, @RequestHeader(value = "Cookie", required = false) String sessionCookie) throws Exception {
        System.out.println("+++++++++ " + applicationHistoryRequestDTO.toString());
        String incomingCookie = this.normalizeIncomingCookie(sessionCookie);
        if (!incomingCookie.isBlank()) {
            // OTP completion calls this endpoint again with the verified MCA cookie.
            System.out.println("Using caller supplied MCA session cookie for applicationstatus");
            return this.fetchApplicationStatusWithAuthenticatedCookie(applicationHistoryRequestDTO, incomingCookie, false);
        }
        LoginResponse loginResponse = this.mcaLoginService.login(applicationHistoryRequestDTO.getUserName(), applicationHistoryRequestDTO.getPassword(), applicationHistoryRequestDTO.getDeviceId(), "login");
        System.out.println("Login Response is " + loginResponse.status() + " cookie is " + loginResponse.cookie());
        if (false == loginResponse.status()) {
            return loginResponse.message();
        }
        if (loginResponse.status() && "Otp Required".equalsIgnoreCase(loginResponse.message())) {
            return loginResponse.cookie();
        }
        return this.fetchApplicationStatusWithAuthenticatedCookie(applicationHistoryRequestDTO, loginResponse.cookie(), true);
    }

    private String fetchApplicationStatusWithAuthenticatedCookie(ApplicationHistoryRequestDTO applicationHistoryRequestDTO, String cookie, boolean logoutWhenDone) throws Exception {
        String responseData = this.applicationStatusService.getApplicationHistory(applicationHistoryRequestDTO, cookie);
        if (logoutWhenDone) {
            this.mcaLoginService.logout(cookie);
        }
        return responseData;
    }

    @PostMapping({"/endtsrn"})
    public String editSRN(@RequestBody EditSRNRequestDto editSRNRequestDto) throws IOException {
        System.out.println("+++++++++ " + editSRNRequestDto.toString());
        LoginResponse loginResponse = this.mcaLoginService.login(editSRNRequestDto.getUserName(), editSRNRequestDto.getPassword(), editSRNRequestDto.getDeviceId(), "login");
        System.out.println("Login Response is " + loginResponse.status() + " cookie is " + loginResponse.cookie());
        if (false == loginResponse.status()) {
            return loginResponse.message();
        }
        if (loginResponse.status() && "Otp Required".equalsIgnoreCase(loginResponse.message())) {
            return loginResponse.cookie();
        }
        String responseData = this.applicationStatusService.editSRN(editSRNRequestDto.getRefNo(), loginResponse.cookie());
        this.mcaLoginService.logout(loginResponse.cookie());
        return responseData;
    }

    @PostMapping({"/downloadall"})
    public ResponseEntity<Resource> downloadAll(@RequestBody DownloadStatusDocumentDTO downloadStatusDocumentDTO, @RequestHeader(value = "Cookie", required = false) String sessionCookie) throws Exception {
        System.out.println("+++++++++ " + downloadStatusDocumentDTO.toString());
        String incomingCookie = this.normalizeIncomingCookie(sessionCookie);
        if (!incomingCookie.isBlank()) {
            // Preserve the caller-owned OTP session while downloading files.
            System.out.println("Using caller supplied MCA session cookie for downloadall");
            return this.downloadAllWithAuthenticatedCookie(downloadStatusDocumentDTO, incomingCookie, false);
        }
        LoginResponse loginResponse = this.mcaLoginService.login(downloadStatusDocumentDTO.getUserName(), downloadStatusDocumentDTO.getPassword(), downloadStatusDocumentDTO.getDeviceId(), "login");
        System.out.println("Login Response is " + loginResponse.status() + " cookie is " + loginResponse.cookie());
        if (false == loginResponse.status()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ByteArrayResource(loginResponse.message().getBytes(StandardCharsets.UTF_8)));
        }
        if (loginResponse.status() && "Otp Required".equalsIgnoreCase(loginResponse.message())) {
            return ResponseEntity.status(HttpStatus.OK).body(new ByteArrayResource(loginResponse.cookie().getBytes(StandardCharsets.UTF_8)));
        }
        return this.downloadAllWithAuthenticatedCookie(downloadStatusDocumentDTO, loginResponse.cookie(), true);
    }

    private ResponseEntity<Resource> downloadAllWithAuthenticatedCookie(DownloadStatusDocumentDTO downloadStatusDocumentDTO, String cookie, boolean logoutWhenDone) throws Exception {
        ResponseEntity<Resource> docList = this.applicationStatusService.docDownloadList(downloadStatusDocumentDTO, cookie);
        if (logoutWhenDone) {
            this.mcaLoginService.logout(cookie);
        }
        return docList;
    }

    @PostMapping({"/fetchannualfillingdoc"})
    public ResponseEntity<Resource> FetchAnnualFilling(@RequestBody MasterDataRequest masterDataReq, @RequestHeader(value = "Cookie", required = false) String sessionCookie) throws Exception {
        System.out.println("+++++++++ " + masterDataReq.toString());
        String incomingCookie = this.normalizeIncomingCookie(sessionCookie);
        if (!incomingCookie.isBlank()) {
            // Annual filing OTP flow reuses the verified cookie for the actual document-list call.
            System.out.println("Using caller supplied MCA session cookie for fetchannualfillingdoc");
            return this.fetchAnnualFillingWithAuthenticatedCookie(masterDataReq, incomingCookie, false);
        }
        LoginResponse loginResponse = this.mcaLoginService.login(masterDataReq.getUserName(), masterDataReq.getPassword(), masterDataReq.getDeviceId(), "login");
        System.out.println("Login Response is " + loginResponse.status() + " cookie is " + loginResponse.cookie());
        if (false == loginResponse.status()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ByteArrayResource(loginResponse.message().getBytes(StandardCharsets.UTF_8)));
        }
        if (loginResponse.status() && "Otp Required".equalsIgnoreCase(loginResponse.message())) {
            return ResponseEntity.status(HttpStatus.OK).body(new ByteArrayResource(loginResponse.cookie().getBytes(StandardCharsets.UTF_8)));
        }
        return this.fetchAnnualFillingWithAuthenticatedCookie(masterDataReq, loginResponse.cookie(), true);
    }

    private ResponseEntity<Resource> fetchAnnualFillingWithAuthenticatedCookie(MasterDataRequest masterDataReq, String cookie, boolean logoutWhenDone) throws Exception {
        ResponseEntity<Resource> docList = this.downloadAnnualFilling.fetchDocumentList(masterDataReq, cookie);
        if (logoutWhenDone) {
            this.mcaLoginService.logout(cookie);
        }
        return docList;
    }

    @GetMapping({"/donwloadannualfillingdoc/{dmsid}"})
    public ResponseEntity<Resource> downloadannualFillingDoc(@PathVariable String dmsid, @RequestHeader(value = "Cookie", required = false) String sessionCookie) throws Exception {
        return this.downloadAnnualFilling.downloadDocuments(dmsid, this.normalizeIncomingCookie(sessionCookie));
    }

    private String normalizeIncomingCookie(String cookie) {
        if (cookie == null) {
            return "";
        }
        return cookie.trim().replace("\r", "").replace("\n", "");
    }
}
