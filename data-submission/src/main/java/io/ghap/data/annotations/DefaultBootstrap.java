package io.ghap.data.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.netflix.governator.guice.annotations.Bootstrap;

/**
 *
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Bootstrap(bootstrap= io.ghap.data.contribution.BasicBootstrap.class)
public @interface DefaultBootstrap {
}
