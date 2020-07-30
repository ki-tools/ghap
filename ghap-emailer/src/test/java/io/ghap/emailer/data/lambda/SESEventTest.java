package io.ghap.emailer.data.lambda;

import com.google.gson.Gson;
import io.ghap.emailer.lambda.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.InputStream;
import java.io.InputStreamReader;

@RunWith(JUnit4.class)
public class SESEventTest {
  @Test
  public void TestSESEvent() {
    InputStream inputStream = SESEventTest.class.getResourceAsStream("/example.json");
    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
    Gson gson = new Gson();
    SESEvent event = gson.fromJson(inputStreamReader, SESEvent.class);
    Assert.assertNotNull(event);
    Assert.assertNotNull(event.getRecords());
    Assert.assertEquals(1, event.getRecords().length);
    SESRecord record = event.getRecords()[0];
    SES ses = record.getSES();
    Assert.assertNotNull(ses);
    SESMail mail = ses.getMail();
    Assert.assertNotNull(mail);
    SESReceipt receipt = ses.getReceipt();
    Assert.assertNotNull(receipt);
  }
}
