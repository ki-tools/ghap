package io.ghap.userdata.contribution.storage.s3;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

public class Utils {
    public static String normalize(String string) {
        try {
            string = new URI(null, null, string, null).normalize().toString();
            return URLDecoder.decode(string, "UTF-8");
            //string = URI.create(URLEncoder.encode(string, "UTF-8")).normalize().toString();
            //return URLDecoder.decode(string, "UTF-8");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public static String correctPath(String path, boolean asFolder) {
        path = normalize(path);
        if (asFolder) { // only folder
            if (path == null || ".".equals(path) || "/".equals(path) || "".equals(path)) {
                path = "";
            }
            else {
                if (path.startsWith("/")) path = path.substring(1);
                if (!path.endsWith("/")) path = path + "/";
            }
        }
        else { // full relative path
            if (path == null || "/".equals(path)) path = ".";
            if (path.startsWith("/")) path = path.substring(1);
        }
        return path;
    }
}
