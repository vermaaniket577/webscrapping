package com.mca.automate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mca.automate.dto.DownloadStatusDocumentDTO;
import com.mca.automate.util.CryptoUtil;
import com.mca.automate.util.Util;
import java.nio.charset.StandardCharsets;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

class MCAApplicationStatusServiceTest {

    @Test
    void docDownloadListHandlesNullDmsIdWithoutThrowing() throws Exception {
        MCAApplicationStatusService service = org.mockito.Mockito.spy(new MCAApplicationStatusService());
        service.util = new Util();
        service.crypto = mock(CryptoUtil.class);
        when(service.crypto.encrypt(anyString())).thenReturn("encrypted");
        doReturn(response(500)).when(service).apiCall(anyString(), eq("session-cookie"));

        DownloadStatusDocumentDTO request = new DownloadStatusDocumentDTO(
                "download",
                "REF123",
                "",
                "",
                null,
                "user@example.com",
                "secret",
                "device-1");

        ResponseEntity<Resource> response = service.docDownloadList(request, "session-cookie");

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(resourceText(response.getBody())).isEqualTo("Kindly Contact to Administrator!!!");
        verify(service).apiCall(anyString(), eq("session-cookie"));
    }

    private Response response(int code) {
        return new Response.Builder()
                .request(new Request.Builder().url("https://www.mca.gov.in/bin/mca/applicationHistory").build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test")
                .body(ResponseBody.create("", MediaType.parse("text/plain")))
                .build();
    }

    private String resourceText(Resource resource) {
        assertThat(resource).isInstanceOf(ByteArrayResource.class);
        return new String(((ByteArrayResource) resource).getByteArray(), StandardCharsets.UTF_8);
    }
}
