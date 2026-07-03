package com.mca.automate.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MasterDataRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void acceptsCompanyNameOrCinCanonicalField() throws Exception {
        MasterDataRequest request = mapper.readValue("""
                {
                  "CompanyNameOrCIN": "L74909DL2008PLC180850",
                  "userName": "user@example.com",
                  "password": "secret",
                  "deviceId": "device-1"
                }
                """, MasterDataRequest.class);

        assertThat(request.getCompanyNameOrCIN()).isEqualTo("L74909DL2008PLC180850");
    }

    @Test
    void acceptsCinAlias() throws Exception {
        MasterDataRequest request = mapper.readValue("""
                {
                  "cin": "L74909DL2008PLC180850",
                  "userName": "user@example.com",
                  "password": "secret",
                  "deviceId": "device-1"
                }
                """, MasterDataRequest.class);

        assertThat(request.getCompanyNameOrCIN()).isEqualTo("L74909DL2008PLC180850");
    }
}
