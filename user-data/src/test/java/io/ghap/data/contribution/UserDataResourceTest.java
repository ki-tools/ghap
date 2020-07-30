package io.ghap.data.contribution;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import governator.junit.GovernatorJunit4Runner;
import governator.junit.config.LifecycleInjectorParams;
import io.ghap.data.contribution.server.BasicTestBootstrap;
import io.ghap.data.contribution.server.UserDataTestBootstrap;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.ghap.userdata.contribution.UserData;
import io.ghap.userdata.contribution.UserDataResource;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.sun.jersey.core.header.FormDataContentDisposition;

@RunWith(GovernatorJunit4Runner.class)
@LifecycleInjectorParams(modules = UserDataTestBootstrap.class, bootstrapModule = BasicTestBootstrap.class, scannedPackages = "io.ghap.userdata.contribution")
public class UserDataResourceTest
{
  @Inject
  private UserDataResource userDataResource;
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void setup()
  {
    System.setProperty("aws.accessKeyId","");
    System.setProperty("aws.secretKey", "");
  }
  
  @Test
  public void testConfigurationBinding()
  {
    assertThat(userDataResource.getServiceName(),equalTo("Test User Data Service"));
  }
  
  @Test
  public void testEmpty()
  {
    Response response = userDataResource.data("0567303f-c587-47d2-8f10-b7756a2f7149", "", "", null);
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Gson gson = new Gson();
    Type listType = new TypeToken<ArrayList<UserData>>() {
    }.getType();
    List<UserData> submission = gson.fromJson(response.getEntity().toString(), listType);
    Assert.assertEquals(0, submission.size());
  }
  
  @Test
  public void testSubmitAndDelete() throws IOException {
    String test = "This is a test file.";
    InputStream stream = new ByteArrayInputStream(test.getBytes());
    FormDataContentDisposition contentDisposition = 
      FormDataContentDisposition.name("file").fileName("test.file").size(test.getBytes().length).build();
    Response response = userDataResource.submit("0567303f-c587-47d2-8f10-b7756a2f7149", stream, contentDisposition);
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    List<String> files = Arrays.asList(new String[] {"test.file"});
    try {
      response = userDataResource.delete("0567303f-c587-47d2-8f10-b7756a2f7149","/", files);
    } catch(IOException ioe) {
      Assert.fail(ioe.getMessage());
    }
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testSubmitListAndDelete() throws IOException {
    String test = "This is a test file.";
    InputStream stream = null;
    try {
      stream = new ByteArrayInputStream(test.getBytes("US-ASCII"));
    } catch(Exception e) {
      Assert.fail(e.getMessage());
    }
    FormDataContentDisposition contentDisposition = 
      FormDataContentDisposition.name("file").fileName("test_file.txt").size(test.getBytes().length).build();
    Response response = userDataResource.submit("0567303f-c587-47d2-8f10-b7756a2f7149", stream, contentDisposition);
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    
    Response in = userDataResource.data("0567303f-c587-47d2-8f10-b7756a2f7149", "test_file.txt", "", null);
    Assert.assertNotNull(in);
    Assert.assertEquals(200, in.getStatus());
    Assert.assertTrue(in.getEntity() instanceof InputStream);

    InputStream reader = new BufferedInputStream((InputStream)in.getEntity());
    try {
      byte[] buffer = new byte[1024];
      int counter = 0;
      StringBuffer content = new StringBuffer();
      for(int read = reader.read(buffer); read > 0; read = reader.read(buffer))
      {
        content.append(new String(buffer, 0, read));
        counter+= read;
      }
      Assert.assertEquals(test.length(), counter);
      Assert.assertEquals(test, content.toString());
    } catch(Exception e) {
      Assert.fail(e.getMessage());
    }

    try {
      reader.close();
    } catch(Exception e) {}
    
    response = userDataResource.data("0567303f-c587-47d2-8f10-b7756a2f7149", "test_file.txt", "", null);
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Assert.assertTrue(in.getEntity() instanceof InputStream);

    Gson gson = new Gson();
    Type listType = new TypeToken<ArrayList<UserData>>() {
    }.getType();
    List<UserData> submissions = gson.fromJson(response.getEntity().toString(), listType);
    Assert.assertEquals(1, submissions.size());
    List<String> files = Arrays.asList(new String[] {"test_file.txt"});
    try {
      response = userDataResource.delete("0567303f-c587-47d2-8f10-b7756a2f7149", "/", files);
    } catch(IOException ioe) {
      Assert.fail(ioe.getMessage());
    }
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testSubmitRetrieveAndDelete() throws IOException {
    String test = "This is a test file.";
    InputStream stream = null;
    try {
      stream = new ByteArrayInputStream(test.getBytes("US-ASCII"));
    } catch(Exception e) {
      Assert.fail(e.getMessage());
    }
    FormDataContentDisposition contentDisposition = 
      FormDataContentDisposition.name("file").fileName("test_file.txt").size(test.getBytes().length).build();
    Response response = userDataResource.submit("0567303f-c587-47d2-8f10-b7756a2f7149",stream, contentDisposition);
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    
    Response in = userDataResource.data("0567303f-c587-47d2-8f10-b7756a2f7149", "test_file.txt", "", null);
    Assert.assertNotNull(in);
    InputStream reader = new BufferedInputStream((InputStream)in.getEntity());
    try {
      byte[] buffer = new byte[1024];
      int counter = 0;
      StringBuffer content = new StringBuffer();
      for(int read = reader.read(buffer); read > 0; read = reader.read(buffer))
      {
        content.append(new String(buffer, 0, read));
        counter+= read;
      }
      Assert.assertEquals(test.length(), counter);
      Assert.assertEquals(test, content.toString());
    } catch(Exception e) {
      Assert.fail(e.getMessage());
    }

    try {
      reader.close();
    } catch(Exception e) {}

    List<String> files = Arrays.asList(new String[] {"test_file.txt"});
    try {
      response = userDataResource.delete("0567303f-c587-47d2-8f10-b7756a2f7149", "/", files);
    } catch(IOException ioe) {
      Assert.fail(ioe.getMessage());
    }
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }
}
