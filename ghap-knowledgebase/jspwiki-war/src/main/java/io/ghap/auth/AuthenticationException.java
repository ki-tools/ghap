package io.ghap.auth;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;

/**
 * <p>A runtime exception representing a failure to provide correct
 * authentication credentials.</p>
 */
public class AuthenticationException extends Exception {

    private final int status;

    public AuthenticationException(String message) {
        this(401, message);
    }
    public AuthenticationException(int status) {
        this(status, "Authentication error " + status);
    }
    public AuthenticationException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
