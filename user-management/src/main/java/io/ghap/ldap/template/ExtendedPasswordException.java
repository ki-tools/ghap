package io.ghap.ldap.template;

import org.apache.directory.ldap.client.template.exception.PasswordException;

/**
 *
 */
public class ExtendedPasswordException extends PasswordException {

    private final String message;

    public ExtendedPasswordException(final String message){
        super();
        this.message = message;
    }

    @Override
    public String getMessage(){
        return message;
    }

}
