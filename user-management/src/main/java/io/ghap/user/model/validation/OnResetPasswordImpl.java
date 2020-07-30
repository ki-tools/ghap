package io.ghap.user.model.validation;


import com.google.inject.Singleton;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Set;


@Singleton
public class OnResetPasswordImpl implements OnResetPassword {

    @Inject Validator validator;

    @Override
    //@Validate(groups=OnResetPassword.class)
    public <T> T validate(T o) {
        Set constraintViolations = validator.validate( o, OnResetPassword.class );
        if( !constraintViolations.isEmpty() ){
            throw new ConstraintViolationException(constraintViolations);
        }
        return o;
    }

}
