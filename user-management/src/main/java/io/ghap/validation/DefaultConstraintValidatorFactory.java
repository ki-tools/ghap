package io.ghap.validation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

@Singleton
public class DefaultConstraintValidatorFactory implements ConstraintValidatorFactory {
    @Inject Injector injector;

    @Override
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
        return injector.getInstance(key);
    }
}
