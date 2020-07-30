package io.ghap.emailer.data;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(JUnit4.class)
public class EmailFactoryImplTest {
  private EmailFactory emailFactory = new EmailFactoryImpl();
  private String INBOUND_1 = "admin@ghap.io";
  private String INBOUND_2 = "stephan.nagy@ghap.io";
  private String OUTBOUND_1 = "admin@certara.com";
  private String OUTBOUND_2 = "stephan.nagy@certara.com";

  @After
  public void CleanUp() {
    emailFactory.removeAllMappedEmailAddresses(INBOUND_1);
    emailFactory.removeAllMappedEmailAddresses(INBOUND_2);
  }

  @Test
  public void TestAllMappedFromEmailAddresses() {
    List<String> allFrom = emailFactory.getAllMappedFromEmailAddresses();
    Assert.assertNotNull(allFrom);
  }

  @Test
  public void TestAddEmailAddressMap() {
    EmailMapping mapping = emailFactory.addMapEmailAddress(INBOUND_1,OUTBOUND_1);
    Assert.assertNotNull(mapping);
    Assert.assertEquals(INBOUND_1, mapping.getInboundEmailAddress());
    Assert.assertEquals(1, mapping.getOutboundEmailAddresses().size());
    List<String> list = new ArrayList<>(mapping.getOutboundEmailAddresses());
    Assert.assertEquals(OUTBOUND_1, list.get(0));
  }

  @Test
  public void TestAddMultipleEmailAddressMap() {
    EmailMapping mapping = emailFactory.addMapEmailAddress(INBOUND_1,OUTBOUND_1);
    mapping = emailFactory.addMapEmailAddress(INBOUND_1, OUTBOUND_2);
    Assert.assertNotNull(mapping);
    Assert.assertEquals(INBOUND_1, mapping.getInboundEmailAddress());
    Assert.assertEquals(2, mapping.getOutboundEmailAddresses().size());
  }

  @Test
  public void TestRemoveSingleOutbound()
  {
    emailFactory.addMapEmailAddress(INBOUND_1, OUTBOUND_1);
    emailFactory.addMapEmailAddress(INBOUND_1, OUTBOUND_2);
    Assert.assertEquals(2, emailFactory.getMappedEmailAddresses(INBOUND_1).size());
    emailFactory.removeMappedEmailAddress(INBOUND_1, OUTBOUND_2);
    Assert.assertEquals(1, emailFactory.getMappedEmailAddresses(INBOUND_1).size());
  }


  @Test
  public void TestGetAllAsMap()
  {
    int initialSize = emailFactory.getAllMappedFromEmailAddresses().size();
    emailFactory.addMapEmailAddress(INBOUND_1, OUTBOUND_1);
    emailFactory.addMapEmailAddress(INBOUND_1, OUTBOUND_2);
    emailFactory.addMapEmailAddress(INBOUND_2, OUTBOUND_2);
    Map<String,Set<String>> map = emailFactory.getAllMappedEmailAddresses();
    Assert.assertNotNull(map);
    Assert.assertEquals(2, map.keySet().size() - initialSize);
    for(String key: map.keySet()) {
      Assert.assertNotNull(map.get(key));
      Assert.assertTrue(map.get(key).size() > 0);
    }
  }

  @Test
  public void TestUpdate()
  {
    EmailMapping mapping = emailFactory.addMapEmailAddress(INBOUND_1, OUTBOUND_1);
    Assert.assertEquals(1, mapping.getOutboundEmailAddresses().size());
    mapping.addOutboundEmailAddress(OUTBOUND_2);
    emailFactory.update(mapping);
    EmailMapping fresh = emailFactory.get(INBOUND_1);
    Assert.assertEquals(2, fresh.getOutboundEmailAddresses().size());
  }
}