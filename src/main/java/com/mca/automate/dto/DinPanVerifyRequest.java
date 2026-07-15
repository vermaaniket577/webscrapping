package com.mca.automate.dto;

import lombok.Data;

/**
 * Request payload for POST /verifydinpan
 * din -> DIN/DPIN of the director/designated partner
 * pan -> Income-tax PAN to verify against the DIN database
 */
@Data
public class DinPanVerifyRequest {
    private String din;
    private String pan;
}
