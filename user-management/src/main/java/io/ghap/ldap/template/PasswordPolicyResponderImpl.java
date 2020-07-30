package io.ghap.ldap.template;

import org.apache.directory.api.ldap.codec.api.LdapApiService;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicy;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.ResultResponse;
import org.apache.directory.ldap.client.template.AbstractPasswordPolicyResponder;
import org.apache.directory.ldap.client.template.PasswordPolicyResponder;
import org.apache.directory.ldap.client.template.exception.PasswordException;


/**
 * The default implementation of {@link PasswordPolicyResponder}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
final class PasswordPolicyResponderImpl extends AbstractPasswordPolicyResponder
        implements PasswordPolicyResponder
{
    PasswordPolicyResponderImpl( LdapApiService ldapApiService )
    {
        super( ldapApiService );
    }

    /**
     * Returns an exception to be thrown in the case of a non SUCCESS
     * <code>resultCode</code>.
     *
     * @param resultResponse
     * @param passwordPolicy
     * @param resultCode
     * @return
     */
    protected PasswordException fail( ResultResponse resultResponse,
                                      PasswordPolicy passwordPolicy, ResultCodeEnum resultCode )
    {
        PasswordException exception = new ExtendedPasswordException(resultResponse.getLdapResult().getDiagnosticMessage());
        exception.setResultCode( resultCode );
        if ( passwordPolicy != null
                && passwordPolicy.getResponse() != null
                && passwordPolicy.getResponse().getPasswordPolicyError() != null )
        {
            exception.setPasswordPolicyError( passwordPolicy.getResponse().getPasswordPolicyError() );
        }
        return exception;
    }

}
