package io.ghap.emailer.data;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ConfigurationFactoryTest {

  private static final String NAME = "emailer.configuration.test";
  private static final String VALUE = "this is a test";

  @After
  public void tearDown() {
    ConfigurationFactory factory = new ConfigurationFactoryImpl();
    for(Configuration configuration : factory.getConfigurations()) {
      factory.remove(configuration);
    }
  }

  @Test
  public void TestGetAllConfiguration() {
    ConfigurationFactory factory = new ConfigurationFactoryImpl();
    Assert.assertNotNull(factory.getConfigurations());
  }

  @Test
  public void TestSetConfiguration() {
    ConfigurationFactory factory = new ConfigurationFactoryImpl();
    int initial_size = factory.getConfigurations().size();
    factory.setConfiguration(NAME, VALUE);
    Configuration configuration = factory.getConfiguration(NAME);
    Assert.assertNotNull(configuration);
    Assert.assertEquals(NAME, configuration.getPropertyName());
    Assert.assertEquals(VALUE,configuration.getPropertyValue());
    Assert.assertEquals(1, factory.getConfigurations().size() - initial_size);
  }
}
