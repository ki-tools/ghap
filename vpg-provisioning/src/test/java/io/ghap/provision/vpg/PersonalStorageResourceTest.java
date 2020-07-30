package io.ghap.provision.vpg;

import com.google.gson.Gson;
import com.sun.jersey.api.client.ClientResponse;
import governator.junit.GovernatorJunit4Runner;
import governator.junit.config.LifecycleInjectorParams;
import io.ghap.provision.vpg.data.PersonalStorageFactory;
import io.ghap.provision.vpg.data.PersonalStorageFactoryMockImpl;
import io.ghap.provision.vpg.guice.VPGServletBootstrapModule;
import io.ghap.provision.vpg.guice.VPGServletModule;
import org.junit.*;
import org.junit.rules.ExpectedException;

import com.google.inject.Inject;
import com.netflix.governator.annotations.Configuration;
import org.junit.runner.RunWith;

import javax.ws.rs.core.Response;
import java.util.UUID;

@RunWith(GovernatorJunit4Runner.class)
@LifecycleInjectorParams(modules = VPGServletModule.class, bootstrapModule = VPGServletBootstrapModule.class, scannedPackages = "io.ghap.provision.vpg")
public class PersonalStorageResourceTest
{
  @Inject
  private PersonalStorageFactory personalStorageFactory;

  @Inject
  private PersonalStorageResource storageResource;
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Configuration("aws.accessKeyId")
  private String keyId;

  @Configuration("aws.secretKey")
  private String secretKey;

  @Configuration("availability.zone")
  private String availabilityZone;

  @Before
  public void setup()
  {
    ((PersonalStorageFactoryMockImpl)personalStorageFactory).setup();
  }

  @After
  public void cleanup()
  {
    ((PersonalStorageFactoryMockImpl)personalStorageFactory).cleanup();
  }

  @Test
  public void testConfigurationBinding()
  {
    Assert.assertEquals(storageResource.getServiceName(),"Test Personal Storage Manager");
  }
  
  @Test
  public void testCreate()
  {
    String uuid = UUID.randomUUID().toString();
    Response response = storageResource.create(uuid, 1);
    Assert.assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
    Gson gson = new Gson();
    PersonalStorage storage = gson.fromJson(response.getEntity().toString(), PersonalStorage.class);
    Assert.assertNotNull(storage.getAvailabilityZone());
    Assert.assertEquals(availabilityZone, storage.getAvailabilityZone());
    Assert.assertNotNull(storage.getVolumeId());
    Assert.assertNotNull(storage.getId());
    Assert.assertNotNull(storage.getUserId());
    Assert.assertEquals(uuid, storage.getUserId().toString());
    Assert.assertEquals(1, storage.getSize());
    response = storageResource.delete(uuid);
    Assert.assertEquals(ClientResponse.Status.OK.getStatusCode(), response.getStatus());
  }
}
