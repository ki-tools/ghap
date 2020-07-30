package io.ghap.auth;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import java.security.cert.X509Certificate;

public class Utils {
    public static final String slashAtTheEnd(String str){
        return str != null && !str.endsWith("/") ? str + "/":str;
    }

    public static boolean equals(MediaType that, MediaType obj) {
        if(obj == null) {
            return false;
        } else if(!(obj instanceof MediaType)) {
            return false;
        } else {
            MediaType other = (MediaType)obj;
            return that.getType().equalsIgnoreCase(other.getType()) && that.getSubtype().equalsIgnoreCase(other.getSubtype());
        }
    }

    // disable cert verification
    public static SSLContext getSSLContext() {

        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("SSL");
            ctx.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (java.security.GeneralSecurityException ex) {
        }
        return ctx;
    }
}
