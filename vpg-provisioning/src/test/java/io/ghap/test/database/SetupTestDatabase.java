package io.ghap.test.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SetupTestDatabase {
    String persistenceUnit();
    String stackPrefix() default "TestProvisioning";
}
