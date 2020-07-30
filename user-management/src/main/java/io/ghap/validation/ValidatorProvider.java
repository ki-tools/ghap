package io.ghap.validation;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

@Singleton
public class ValidatorProvider implements Provider<Validator> {
    private Validator validator;

    @Inject
    public ValidatorProvider(DefaultConstraintValidatorFactory constraintValidatorFactory){
        ValidatorFactory factory = Validation.byDefaultProvider()
                .configure()
                .constraintValidatorFactory(constraintValidatorFactory)
                .buildValidatorFactory();

        this.validator = factory.getValidator();
    }
    @Override
    public Validator get() {
        return validator;
    }
}
