package io.ghap.emailer.data;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;

import java.util.*;

public class EmailFactoryImpl implements EmailFactory
{
  private AmazonDynamoDBClient client;

  @Override
  public EmailMapping addMapEmailAddress(String from, String to) {
    AmazonDynamoDBClient client = getClient();
    DynamoDBMapper mapper = new DynamoDBMapper(client);
    EmailMapping mapping = fetchEmailMapping(from);
    if(mapping == null) {
      mapping = new EmailMapping();
      mapping.setInboundEmailAddress(from);
    }
    mapping.addOutboundEmailAddress(to);
    mapper.save(mapping);
    return mapping;
  }

  @Override
  public void removeMappedEmailAddress(String from, String to) {
    AmazonDynamoDBClient client = getClient();
    DynamoDBMapper mapper = new DynamoDBMapper(client);

    EmailMapping mapping = fetchEmailMapping(from);
    if(mapping != null) {
      mapping.removeOutboundEmailAddress(to);
      if(mapping.getOutboundEmailAddresses().size() == 0) {
        mapper.delete(mapping);
      } else {
        mapper.save(mapping);
      }
    }
  }

  @Override
  public void removeAllMappedEmailAddresses(String from) {
    AmazonDynamoDBClient client = getClient();
    DynamoDBMapper mapper = new DynamoDBMapper(client);

    EmailMapping mapping = fetchEmailMapping(from);
    if(mapping != null) {
      mapper.delete(mapping);
    }
  }

  @Override
  public Set<String> getMappedEmailAddresses(String from) {
    EmailMapping mapping = fetchEmailMapping(from);
    return mapping != null ? mapping.getOutboundEmailAddresses() : new LinkedHashSet<>();
  }

  @Override
  public List<String> getAllMappedFromEmailAddresses() {
    List<EmailMapping> mappings = fetchAllEmailMappings();
    List<String> from = new ArrayList<>();
    for(EmailMapping mapping: mappings) {
      from.add(mapping.getInboundEmailAddress());
    }
    return from;
  }

  @Override
  public Map<String, Set<String>> getAllMappedEmailAddresses() {
    List<EmailMapping> mappings = fetchAllEmailMappings();
    Map<String,Set<String>> mappingsMap = new HashMap<>();
    for(EmailMapping mapping: mappings) {
      mappingsMap.put(mapping.getInboundEmailAddress(), mapping.getOutboundEmailAddresses());
    }
    return mappingsMap;
  }

  @Override
  public void update(EmailMapping mapping) {
    AmazonDynamoDBClient client = getClient();
    DynamoDBMapper mapper = new DynamoDBMapper(client);

    mapper.save(mapping);
  }

  @Override
  public EmailMapping get(String from) {
    return fetchEmailMapping(from);
  }

  private List<EmailMapping> fetchAllEmailMappings() {
    AmazonDynamoDBClient client = getClient();
    DynamoDBMapper mapper = new DynamoDBMapper(client);

    DynamoDBScanExpression expression = new DynamoDBScanExpression();
    List<EmailMapping> mappings = mapper.scan(EmailMapping.class, expression);
    return mappings;
  }

  private EmailMapping fetchEmailMapping(String from) {
    AmazonDynamoDBClient client = getClient();
    DynamoDBMapper mapper = new DynamoDBMapper(client);
    EmailMapping mapping = mapper.load(EmailMapping.class, from);
    return mapping;
  }

  private AmazonDynamoDBClient getClient() {
    if(client == null) {
      client = new AmazonDynamoDBClient(new DefaultAWSCredentialsProviderChain());
    }
    return client;
  }
}
