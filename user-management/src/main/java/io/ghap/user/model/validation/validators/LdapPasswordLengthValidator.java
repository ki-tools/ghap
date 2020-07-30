package io.ghap.user.model.validation.validators;

import com.google.inject.Inject;
import io.ghap.auth.LdapConfiguration;
import io.ghap.auth.LdapPrincipal;
import io.ghap.ldap.LdapConnectionFactory;
import io.ghap.ldap.LdapUtils;
import io.ghap.ldap.template.LdapConnectionTemplate;
import io.ghap.user.dao.mapper.DomainEntryMapper;
import io.ghap.user.model.Domain;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.template.exception.LdapRuntimeException;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ValidationException;
import java.io.IOException;

public class LdapPasswordLengthValidator implements PasswordLengthValidator {

    // here we can use only "bootstrap" bindings
    @Inject LdapConfiguration ldapConfiguration;
    @Inject LdapConnectionFactory connectionFactory;

    protected int min;
    protected int max;

    @Override
    public void initialize(PasswordLength constraint) {


        try {
            LdapPrincipal principal = ldapConfiguration.getAdmin();
            String realm = ldapConfiguration.getLdapRealm();
            Dn dn = LdapUtils.toDn(realm);

            try(LdapConnection ldap = connectionFactory.get(principal)){
                LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);
                Domain domain = connectionTemplate.lookup(dn, DomainEntryMapper.getInstance());
                this.min = domain.getMinPwdLength();
            } catch (IOException e) {
                throw new LdapRuntimeException(new LdapException(e));
            }

        } catch (LdapException e) {
            throw new ValidationException("Cannot retrieve \"minPwdLength\" from AD", e);
        }

        this.max = constraint.max();
        if(this.min < 0) {
            throw new ValidationException("Min cannot be negative");
        } else if(this.max < 0) {
            throw new ValidationException("Max cannot be negative");
        } else if(this.max < this.min) {
            throw new ValidationException("Max cannot be less than Min");
        }
    }

    public boolean isValid(String s, ConstraintValidatorContext context) {
        if(s == null) {
            return true;
        } else {
            int length = s.length();
            boolean valid = length >= this.min && length <= this.max;
            if( !valid ){
                context.disableDefaultConstraintViolation();
                context
                        .buildConstraintViolationWithTemplate(String.valueOf(min))
                        .addConstraintViolation();
            }
            return valid;
        }
    }
}
