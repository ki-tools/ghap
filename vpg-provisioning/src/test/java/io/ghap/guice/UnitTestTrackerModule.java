package io.ghap.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.matcher.Matchers;
import org.junit.Test;

/**
 * A module that configures the <code>UnitTestTracker</code> to work on methods annotated with <code>@Test</code>
 */
public class UnitTestTrackerModule extends AbstractModule {

  @Inject
  private Injector injector;

  public void configure() {
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(Test.class), new UnitTestTracker());
  }
}
