package com.mca.automate.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UtilTest {

    private final Util util = new Util();

    @Test
    void extractCinReturnsRequestedCinEvenWhenSearchFirstResultDiffers() throws Exception {
        String searchResponse = """
                {
                  "data": {
                    "result": [
                      { "cnNmbr": "U00000AA2000PLC000000" },
                      { "cnNmbr": "L74909DL2008PLC180850" }
                    ]
                  }
                }
                """;

        String cin = util.extractCIN(searchResponse, "L74909DL2008PLC180850");

        assertThat(cin).isEqualTo("L74909DL2008PLC180850");
    }

    @Test
    void extractCinUsesExactCinInputWithoutDependingOnSearchResponse() throws Exception {
        String cin = util.extractCIN("not-json", "L74909DL2008PLC180850");

        assertThat(cin).isEqualTo("L74909DL2008PLC180850");
    }
}
