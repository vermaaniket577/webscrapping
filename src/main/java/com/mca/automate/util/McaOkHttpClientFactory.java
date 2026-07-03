package com.mca.automate.util;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class McaOkHttpClientFactory {
    private static final Logger log = LoggerFactory.getLogger(McaOkHttpClientFactory.class);
    private static volatile boolean trustAllWarningLogged = false;

    private McaOkHttpClientFactory() {
    }

    public static OkHttpClient.Builder newBuilder() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (isTrustAllEnabled()) {
            configureTrustAll(builder);
        }
        return builder;
    }

    private static boolean isTrustAllEnabled() {
        return "true".equalsIgnoreCase(System.getenv("MCA_TLS_TRUST_ALL"));
    }

    private static void configureTrustAll(OkHttpClient.Builder builder) {
        try {
            X509TrustManager trustAllManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllManager}, new SecureRandom());
            HostnameVerifier trustAllHosts = (hostname, session) -> true;
            builder.sslSocketFactory(sslContext.getSocketFactory(), trustAllManager);
            builder.hostnameVerifier(trustAllHosts);
            logTrustAllWarning();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to configure dev-only MCA TLS trust-all client", ex);
        }
    }

    private static void logTrustAllWarning() {
        if (!trustAllWarningLogged) {
            trustAllWarningLogged = true;
            log.warn("MCA_TLS_TRUST_ALL=true; HTTPS certificate validation is disabled for MCA HTTP calls. Use only for local proxy/JDK truststore debugging.");
        }
    }
}
