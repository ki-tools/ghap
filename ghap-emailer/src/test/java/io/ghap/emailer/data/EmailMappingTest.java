package io.ghap.emailer.data;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class EmailMappingTest {
  private EmailMapping mapping;
  private String INBOUND = "admin@ghap.io";
  private String OUTBOUND_1 = "admin@certara.com";
  private String OUTBOUND_2 = "tom@certara.com";

  @Before
  public void setUp()
  {
    mapping = new EmailMapping();
    mapping.setInboundEmailAddress(INBOUND);
    mapping.addOutboundEmailAddress(OUTBOUND_1);
  }

  @Test
  public void TestFromEmail() {
    Assert.assertNotNull(mapping.getInboundEmailAddress());
    Assert.assertEquals(INBOUND, mapping.getInboundEmailAddress());
  }

  @Test
  public void TestToEmail() {
    Assert.assertNotNull(mapping.getOutboundEmailAddresses());
    Assert.assertEquals(1, mapping.getOutboundEmailAddresses().size());
    List<String> list = new ArrayList<String>(mapping.getOutboundEmailAddresses());
    Assert.assertEquals(OUTBOUND_1,list.get(0));
  }

  @Test
  public void TestMultipleToEmail() {
    mapping.addOutboundEmailAddress(OUTBOUND_2);
    Assert.assertNotNull(mapping.getOutboundEmailAddresses());
    Assert.assertEquals(2, mapping.getOutboundEmailAddresses().size());
    List<String> list = new ArrayList<String>(mapping.getOutboundEmailAddresses());
    Assert.assertEquals(OUTBOUND_1, list.get(0));
    Assert.assertEquals(OUTBOUND_2, list.get(1));
  }
}