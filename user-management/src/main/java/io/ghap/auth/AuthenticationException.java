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
public class AuthenticationException extends WebApplicationException {

    public AuthenticationException(String message, String realm) {
        super(
                Response.status(UNAUTHORIZED)
                        .type(TEXT_PLAIN_TYPE)
                        .entity(message).build()
        );
        this.realm = realm;
    }

    private String realm = null;

    public String getRealm() {
        return this.realm;
    }

}