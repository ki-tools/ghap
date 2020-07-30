package io.ghap.userdata.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.netflix.governator.guice.annotations.Bootstrap;
import io.ghap.userdata.contribution.BasicBootstrap;

/**
 *
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Bootstrap(bootstrap= BasicBootstrap.class)
public @interface DefaultBootstrap {
}
