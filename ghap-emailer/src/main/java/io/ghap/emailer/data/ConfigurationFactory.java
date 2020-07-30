package io.ghap.emailer.data;

import java.util.List;

public interface ConfigurationFactory {
  Configuration getConfiguration(String propertyName);
  List<Configuration> getConfigurations();
  Configuration setConfiguration(String propertyName, String propertyValue);
  Configuration update(Configuration configuration);
  void remove(Configuration configuration);
  void removeConfiguration(String propertyName);
}
