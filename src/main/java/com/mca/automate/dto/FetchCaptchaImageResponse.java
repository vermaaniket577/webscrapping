package com.mca.automate.dto;

public record FetchCaptchaImageResponse(String base64Image, String preCt, String cookie, boolean status) {
}
