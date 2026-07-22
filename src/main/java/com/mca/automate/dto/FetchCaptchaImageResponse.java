package com.mca.automate.dto;

public record FetchCaptchaImageResponse(String base64Image, String preCt, boolean status, String cookie) {
}
