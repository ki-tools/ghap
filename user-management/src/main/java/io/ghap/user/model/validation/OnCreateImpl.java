package io.ghap.user.model.validation;

//import org.apache.bval.guice.Validate;


import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Set;

@Singleton
public class OnCreateImpl implements OnCreate {

    @Inject Validator validator;

    @Override
    //@Validate(groups=OnResetPassword.class)
    public <T> T validate(T o) {
        Set constraintViolations = validator.validate( o, OnCreate.class );
        if( !constraintViolations.isEmpty() ){
            throw new ConstraintViolationException(constraintViolations);
        }
        return o;
    }
}
