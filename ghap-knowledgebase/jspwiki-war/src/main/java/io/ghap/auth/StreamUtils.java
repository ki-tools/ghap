package io.ghap.auth;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class StreamUtils {
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
}
