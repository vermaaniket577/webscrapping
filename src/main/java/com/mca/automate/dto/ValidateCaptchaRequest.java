package com.mca.automate.dto;

public record ValidateCaptchaRequest(String captchaTxt, String preCt, boolean status, String cookie) {
}
