package io.ghap.web.annotations;

import com.netflix.governator.guice.annotations.Bootstrap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Bootstrap(bootstrap= io.ghap.web.Bootstrap.class)
public @interface DefaultBootstrap {
}
