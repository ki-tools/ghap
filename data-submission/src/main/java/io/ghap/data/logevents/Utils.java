package io.ghap.data.logevents;

import org.apache.commons.io.IOUtils;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

public class Utils {

    public static String encodeURIComponent(String s) {
        String result;

        try {
            result = URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            result = s;
        }

        return result;
    }

    public static final String slashAtTheEnd(String str){
        return str != null && !str.endsWith("/") ? str + "/":str;
    }
    /**
     * Read UTF-8 stream to string and close the stream
     * @param inputStream to read
     * @return string
     * @throws IOException
     */
    public static String toUtfString(InputStream inputStream) throws IOException {
        return toString(inputStream, "UTF-8");
    }

    /**
     * Read encoded stream to string and close the stream
     * @param inputStream to read
     * @param enc - stream charset name
     * @return string
     * @throws IOException
     */
    public static String toString(InputStream inputStream, String enc) throws IOException {
        if (inputStream == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            IOUtils.copy(inputStream, baos);
            return new String(baos.toByteArray(), Charset.forName(enc));
        } finally {
            IOUtils.closeQuietly(baos);
            IOUtils.closeQuietly(inputStream);
        }
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

}
