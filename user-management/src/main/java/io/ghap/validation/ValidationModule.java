package io.ghap.validation;

import com.google.inject.AbstractModule;

import javax.validation.Validator;

/**
 *
 */
public class ValidationModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DefaultConstraintValidatorFactory.class);
        bind(Validator.class).toProvider(ValidatorProvider.class);
    }
}
