package com.mca.automate.util;

import java.util.Map;
import org.springframework.stereotype.Component;

/* JADX INFO: loaded from: MimePart.class */
@Component
public class MimePart {
    public final Map<String, String> headers;
    public final byte[] body;

    MimePart(Map<String, String> headers, byte[] body) {
        this.headers = headers;
        this.body = body;
    }
}
