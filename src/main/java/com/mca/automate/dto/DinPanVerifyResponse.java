package com.mca.automate.dto;

/**
 * status is one of: VERIFIED, UNVERIFIED, PAN_NOT_FOUND, INVALID_DIN, ERROR
 * directorName is the name returned by MCA against the DIN (step 1 lookup)
 * rawMessage is the literal message text MCA returned, kept for debugging/audit
 */
public record DinPanVerifyResponse(String status, String din, String pan, String directorName, String message) {
}
