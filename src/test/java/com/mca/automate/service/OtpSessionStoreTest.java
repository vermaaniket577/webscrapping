package com.mca.automate.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OtpSessionStoreTest {

    @Test
    void remembersSblUserIdByUuidHashAcrossCookieUpdates() {
        OtpSessionStore store = new OtpSessionStore();

        store.remember("__UUID-HASH=abc123; _csrf=first", "1-C4Y6CSU");

        assertThat(store.sblUserId("_csrf=changed; __UUID-HASH=abc123; bm_sv=updated"))
                .isEqualTo("1-C4Y6CSU");
    }

    @Test
    void fallsBackToCsrfWhenUuidHashIsMissing() {
        OtpSessionStore store = new OtpSessionStore();

        store.remember("_csrf=csrf-1; ak_bmsc=value", "1-C4Y6CSU");

        assertThat(store.sblUserId("bm_sv=updated; _csrf=csrf-1"))
                .isEqualTo("1-C4Y6CSU");
    }
}
