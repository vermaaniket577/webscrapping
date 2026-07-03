package com.mca.automate.dto;

public record ValidateCaptchaResponse(String captcha, String preCt, String cookie, boolean status) {
}
