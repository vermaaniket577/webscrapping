package com.mca.automate.dto;

public record FetchCaptchaResponse(String captcha, String preCt, boolean status, String cookie) {
}
