package io.ghap.data.contribution;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import governator.junit.GovernatorJunit4Runner;
import governator.junit.config.LifecycleInjectorParams;
import io.ghap.data.contribution.server.BasicTestBootstrap;
import io.ghap.data.contribution.server.DataSubmissionTestBootstrap;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

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
@LifecycleInjectorParams(modules = DataSubmissionTestBootstrap.class, bootstrapModule = BasicTestBootstrap.class, scannedPackages = "io.ghap.data.contribution")
public class DataSubmissionResourceTest
{
  @Inject
  private DataSubmissionResource submissionResource;
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Inject
  private SecurityContext context;

  @BeforeClass
  public static void setup()
  {
    System.setProperty("aws.accessKeyId","");
    System.setProperty("aws.secretKey", "");
  }
  
  @Test
  public void testConfigurationBinding()
  {
    assertThat(submissionResource.getServiceName(),equalTo("Test Data Submission Service"));
  }
  
  @Test
  public void testEmpty()
  {    
    Response response = submissionResource.submissions();
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Gson gson = new Gson();
    Type listType = new TypeToken<ArrayList<Submission>>() {
    }.getType();
    List<Submission> submission = gson.fromJson(response.getEntity().toString(), listType);
    Assert.assertEquals(0, submission.size());
  }
  
  @Test
  public void testSubmitAndDelete()
  {    
    String test = "This is a test file.";
    InputStream stream = new ByteArrayInputStream(test.getBytes());
    FormDataContentDisposition contentDisposition = 
      FormDataContentDisposition.name("file").fileName("test.file").size(test.getBytes().length).build();
    Response response = submissionResource.contribute(stream, contentDisposition, context);
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    response = submissionResource.delete("test.file");
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testSubmitListAndDelete()
  {    
    String test = "This is a test file.";
    InputStream stream = null;
    try {
      stream = new ByteArrayInputStream(test.getBytes("US-ASCII"));
    } catch(Exception e) {
      Assert.fail(e.getMessage());
    }
    FormDataContentDisposition contentDisposition = 
      FormDataContentDisposition.name("file").fileName("test_file.txt").size(test.getBytes().length).build();
    Response response = submissionResource.contribute(stream, contentDisposition, context);
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    
    InputStream in = submissionResource.submission("test_file.txt", "", null, null);
    Assert.assertNotNull(in);
    InputStream reader = new BufferedInputStream(in);
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
    
    response = submissionResource.submissions();
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Gson gson = new Gson();
    Type listType = new TypeToken<ArrayList<Submission>>() {
    }.getType();
    List<Submission> submissions = gson.fromJson(response.getEntity().toString(), listType);
    Assert.assertEquals(1, submissions.size());
    response = submissionResource.delete("test_file.txt");
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }
  
  @Test
  public void testSubmitRetrieveAndDelete()
  {    
    String test = "This is a test file.";
    InputStream stream = null;
    try {
      stream = new ByteArrayInputStream(test.getBytes("US-ASCII"));
    } catch(Exception e) {
      Assert.fail(e.getMessage());
    }
    FormDataContentDisposition contentDisposition = 
      FormDataContentDisposition.name("file").fileName("test_file.txt").size(test.getBytes().length).build();
    Response response = submissionResource.contribute(stream, contentDisposition, context);
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    
    InputStream in = submissionResource.submission("test_file.txt", "", null, null);
    Assert.assertNotNull(in);
    InputStream reader = new BufferedInputStream(in);
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
    
    response = submissionResource.delete("test_file.txt");
    Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
  }
}
