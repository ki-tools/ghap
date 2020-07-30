package io.ghap.emailer.data;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;

import java.util.List;

public class ConfigurationFactoryImpl implements ConfigurationFactory {
  private AmazonDynamoDBClient client;

  @Override
  public Configuration setConfiguration(String propertyName, String propertyValue) {
    Configuration configuration = fetchConfigurationEntry(propertyName);
    if(configuration == null) {
      configuration = new Configuration();
      configuration.setPropertyName(propertyName);
    }
    configuration.setPropertyValue(propertyValue);
    return update(configuration);
  }

  @Override
  public Configuration update(Configuration configuration) {
    AmazonDynamoDBClient client = getClient();
    DynamoDBMapper mapper = new DynamoDBMapper(client);

    mapper.save(configuration);
    return configuration;
  }

  @Override
  public Configuration getConfiguration(String property_name) {
    return fetchConfigurationEntry(property_name);
  }

  @Override
  public List<Configuration> getConfigurations() { return fetchAllConfigurationEntries(); }

  @Override
  public void remove(Configuration configuration) {
    AmazonDynamoDBClient client = getClient();
    DynamoDBMapper mapper = new DynamoDBMapper(client);
    mapper.delete(configuration);
  }

  @Override
  public void removeConfiguration(String propertyName) {
    Configuration configuration = getConfiguration(propertyName);
    if(configuration != null) {
      remove(configuration);
    }
  }

  private List<Configuration> fetchAllConfigurationEntries() {
    AmazonDynamoDBClient client = getClient();
    DynamoDBMapper mapper = new DynamoDBMapper(client);

    DynamoDBScanExpression expression = new DynamoDBScanExpression();
    List<Configuration> configurations = mapper.scan(Configuration.class, expression);
    return configurations;
  }

  private Configuration fetchConfigurationEntry(String property_name) {
    AmazonDynamoDBClient client = getClient();
    DynamoDBMapper mapper = new DynamoDBMapper(client);
    Configuration configuration = mapper.load(Configuration.class, property_name);
    return configuration;
  }

  private AmazonDynamoDBClient getClient() {
    if(client == null) {
      client = new AmazonDynamoDBClient(new DefaultAWSCredentialsProviderChain());
    }
    return client;
  }

}
