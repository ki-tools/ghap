package io.ghap.web.tests.validation;

import io.ghap.user.model.validation.validators.PasswordLength;
import io.ghap.user.model.validation.validators.PasswordLengthValidator;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ValidationException;

public class TestPasswordLengthValidator implements PasswordLengthValidator {

    protected int min=8;
    protected int max;

    @Override
    public void initialize(PasswordLength constraint) {

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
