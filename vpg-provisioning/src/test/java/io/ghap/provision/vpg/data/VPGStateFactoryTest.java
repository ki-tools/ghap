package io.ghap.provision.vpg.data;

import com.amazonaws.AmazonServiceException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.netflix.governator.annotations.Configuration;
import governator.junit.config.LifecycleInjectorParams;
import io.ghap.Injectable;
import io.ghap.guice.UnitTestTrackerModule;
import io.ghap.provision.vpg.Activity;
import io.ghap.provision.vpg.PersonalStorage;
import io.ghap.provision.vpg.VirtualPrivateGrid;
import io.ghap.provision.vpg.VirtualPrivateGridStateEntry;
import io.ghap.provision.vpg.guice.TestModule;
import io.ghap.provision.vpg.guice.VPGBootstrapModule;
import io.ghap.test.GovernatorParallelRunner;
import io.ghap.test.MethodModules;
import io.ghap.test.database.SetupTestDatabase;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Ignore
@RunWith(GovernatorParallelRunner.class)
@LifecycleInjectorParams(modules = {UnitTestTrackerModule.class}, bootstrapModule = VPGBootstrapModule.class, scannedPackages = "io.ghap")
public class VPGStateFactoryTest implements Injectable
{
  private Logger logger = LoggerFactory.getLogger(VPGStateFactoryTest.class);

  private static final String TEMPLATE_ANALYSIS_LINUX_ACTIVITY = "https://s3.amazonaws.com/ghap-provisioning-templates-us-east-1-samba/ghap-linux-host-v2.03-SAMBA.json";

  private static long TIMEOUT = 1000 * 60 * 15; // 5 minutes

  @Inject
  private Injector injector;

  private Injector childInjector;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Configuration("aws.accessKeyId")
  private String keyId;

  @Configuration("aws.secretKey")
  private String secretKey;

  @Configuration("availability.zone")
  private String availabilityZone;

  @SetupTestDatabase(stackPrefix = "TestStateRetrieval", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestStateRetrieval()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();
    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_LINUX_ACTIVITY);

    createPersonalStorage(user_uuid, 1);
    VirtualPrivateGrid vpg = vpgFactory.create("test-user", "test-user@nowhere.com", user_uuid, activity);
    waitForGridToTransitionToACreatedState(user_uuid, activity);

    Assert.assertNotNull(vpg);
    List<VirtualPrivateGrid> virtual_envs = vpgFactory.get();
    Assert.assertEquals(1, virtual_envs.size());


    try {

      VPGStateFactory vpgStateFactory = whichInjector().getInstance(VPGStateFactory.class);

      Calendar calendar = Calendar.getInstance();
      Date timeCurrent = calendar.getTime();

      calendar.setTime(timeCurrent);
      calendar.add(Calendar.DAY_OF_YEAR, -1);
      Date timeOneDayBack = calendar.getTime();

      assertThatEntriesExist("get() : Expected vpg state entries", vpgStateFactory.get());
      assertThatEntriesExist("get(stackId) : Expected vpg state entries", vpgStateFactory.get(vpg.getStackId()));
      assertThatEntriesExist("get(userId) : Expected vpg state entries", vpgStateFactory.get(vpg.getUserId()));
      assertThatEntriesExist("get(userId, activityId) : Expected vpg state entries",
              vpgStateFactory.get(vpg.getUserId(), vpg.getActivityId()));

      assertThatEntriesExist("get(startTime, endTime) : Expected vpg state entries",
              vpgStateFactory.get(timeOneDayBack, timeCurrent));
      assertThatEntriesExist("get(stackId, startTime, endTime) : Expected vpg state entries",
              vpgStateFactory.get(vpg.getStackId(), timeOneDayBack, timeCurrent));
      assertThatEntriesExist("get(userId, startTime, endTime) : Expected vpg state entries",
              vpgStateFactory.get(vpg.getUserId(), timeOneDayBack, timeCurrent));
      assertThatEntriesExist("get(userId, activityId, startTime, endTime) : Expected vpg state entries",
              vpgStateFactory.get(vpg.getUserId(), vpg.getActivityId(), timeOneDayBack, timeCurrent));



      assertThatEntriesExist("getActivities() : Expected activities", vpgStateFactory.getActivities());
      assertThatEntriesExist("getActivities(userId) : Expected activities", vpgStateFactory.getActivities(vpg.getUserId()));

      assertThatEntriesExist("getActivities(startTime, endTime) : Expected activities",
              vpgStateFactory.getActivities(timeOneDayBack, timeCurrent));
      assertThatEntriesExist("getActivities(userId, startTime, endTime) : Expected activities",
              vpgStateFactory.getActivities(vpg.getUserId(), timeOneDayBack, timeCurrent));


      assertThatEntriesExist("getUsers() : Expected users", vpgStateFactory.getUsers());
      assertThatEntriesExist("getUsers(startTime, endTime) : Expected users", vpgStateFactory.getUsers(timeOneDayBack, timeCurrent));

      assertThatEntriesExist("getStackNames() : Expected stack names", vpgStateFactory.getStackNames());
      assertThatEntriesExist("getStackNames(startTime, endTime) : Expected stack names", vpgStateFactory.getStackNames(timeOneDayBack, timeCurrent));


      Date creationDate = vpgStateFactory.getCreationDate(vpg.getStackId(), vpg.getUserId());
      Assert.assertNotNull("Expected creation date", creationDate);


      VirtualPrivateGridStateEntry activeState = vpgStateFactory.getActiveState(vpg.getStackId(), vpg.getUserId(), timeCurrent);
      Assert.assertNotNull("Expected active state", activeState);


    } finally {

      vpgFactory.delete(user_uuid, activity.getId());
      virtual_envs = vpgFactory.get();
      Assert.assertEquals(0, virtual_envs.size());

      waitForStorage(user_uuid);
      removePersonalStorage(user_uuid);

    }
  }

  private static void assertThatEntriesExist(String message, List entries) {
    Assert.assertTrue(message, (entries != null && (!entries.isEmpty())));
  }

  private PersonalStorage createPersonalStorage(UUID userIdentifier, int size)
  {
    PersonalStorageFactory personalStorageFactory = whichInjector().getInstance(PersonalStorageFactoryImpl.class);
    PersonalStorage storage = personalStorageFactory.create(userIdentifier, 1);

    Assert.assertNotNull(storage);
    Assert.assertTrue(personalStorageFactory.exists(userIdentifier));
    waitForStorage(userIdentifier);

    return storage;
  }

  private void removePersonalStorage(UUID userIdentifer)
  {
    PersonalStorageFactory personalStorageFactory = whichInjector().getInstance(PersonalStorageFactoryImpl.class);
    if(!personalStorageFactory.available(userIdentifer))
    {
      personalStorageFactory.detach(userIdentifer);
    }

    long sleep_time = 1;
    boolean available = personalStorageFactory.available(userIdentifer);
    while(!available)
    {
      try {
        available = personalStorageFactory.available(userIdentifer);
      } catch(AmazonServiceException ase) {
        if(ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling"))
        {
          sleep_time=sleep_time*2;
          continue;
        }
        throw ase;
      }
      try {
        Thread.sleep(sleep_time);
      } catch(InterruptedException e) {
        // don't care
      }
    }
    personalStorageFactory.delete(userIdentifer);
  }

  private void waitForStorage(UUID user_uuid)
  {
    PersonalStorageFactory personalStorageFactory = whichInjector().getInstance(PersonalStorageFactory.class);
    long start = System.currentTimeMillis();
    long sleep_time = 100;
    for(boolean available = personalStorageFactory.available(user_uuid); !available; available = personalStorageFactory.available(user_uuid))
    {
      try {
        Thread.sleep(sleep_time);
      } catch(InterruptedException ie) {
        // ignore
      } catch(AmazonServiceException ase) {
        if(ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling")) {
          sleep_time*=2;
          continue;
        }
        throw ase;
      }
      long elapsed = System.currentTimeMillis() - start;
      if(elapsed >= TIMEOUT) {
        if(logger.isDebugEnabled()) {
          logger.debug("Reached timeout while waiting for storage operation");
        }
        break;
      }
    }
  }

  private void assertThatGridResourcesAreCreated(UUID user_uuid, Activity activity)
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);
    Assert.assertEquals("CREATE_COMPLETE", vpgFactory.status(user_uuid, activity));
  }

  private void waitForGridToTransitionToARunningState(VirtualPrivateGrid vpg)
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    long start = System.currentTimeMillis();
    int sleep_time = 500;

    for(long duration = 0; duration <= TIMEOUT; duration = System.currentTimeMillis() - start) {
      try {
        boolean continue_checking = true;
        List<VirtualResource> virtual_resources = vpgFactory.getVirtualResources(vpg);
        for (VirtualResource resource : virtual_resources) {
          boolean running = resource.getStatus().equalsIgnoreCase("running");
          continue_checking = continue_checking && !running;
        }
        if(!continue_checking) {
          break;
        }
      }
      catch(AmazonServiceException ase) {
        if(ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling"))
        {
          sleep_time=sleep_time*2;
          continue;
        }
        break;
      }
      try {
        Thread.sleep(sleep_time);
      } catch(InterruptedException ie) {
        // don't care
      }
    }
  }

  private void waitForGridToTransitionToACreatedState(UUID userIdentifer, Activity activity)
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    long start = System.currentTimeMillis();
    long sleep_time = 1;

    for (long duration = 0; duration <= TIMEOUT; duration = System.currentTimeMillis() - start)
    {
      try
      {
        String status = vpgFactory.status(userIdentifer, activity);
        if (status.endsWith("_COMPLETE"))
        {
          break;
        }
      } catch (AmazonServiceException ase)
      {
        if (ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling"))
        {
          sleep_time = sleep_time * 2;
          continue;
        }
        break;
      }
      try
      {
        Thread.sleep(sleep_time);
      } catch (InterruptedException ignored){}
    }

  }

  private void waitForGridToTransitionToAStoppedState(VirtualPrivateGrid vpg)
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);
    long start = System.currentTimeMillis();
    long sleep_time = 1;

    for(long duration = 0; duration < TIMEOUT; duration = System.currentTimeMillis() - start) {
      try {
        boolean continue_checking = true;
        List<VirtualResource> resources = vpgFactory.getVirtualResources(vpg);
        for (VirtualResource resource : resources) {
          continue_checking = continue_checking && !resource.getStatus().equalsIgnoreCase("stopped");
        }
        if(!continue_checking) {
          break;
        }
      }
      catch(AmazonServiceException ase) {
        if(ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling"))
        {
          sleep_time=sleep_time*2;
          continue;
        }
        break;
      }
      try { Thread.sleep(sleep_time);} catch(InterruptedException ignored) {}
    }

  }

  private void assertThatGridResourcesAreRunning(VirtualPrivateGrid vpg)
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);
    List<VirtualResource> virtual_resources = vpgFactory.getVirtualResources(vpg);
    for(VirtualResource resource: virtual_resources) {
      Assert.assertEquals("running", resource.getStatus());
    }
  }

  private void assertThatGridResourcesAreStopped(VirtualPrivateGrid vpg)
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);
    List<VirtualResource> resources = vpgFactory.getVirtualResources(vpg);
    for(VirtualResource resource: resources) {
      Assert.assertEquals("stopped", resource.getStatus());
    }
  }

  private Activity createActivity(String activityName, String templateUrl)
  {
    Activity activity = new Activity();
    activity.setActivityName(activityName);
    activity.setId(UUID.randomUUID());
    activity.setDefaultComputationalUnits(1);
    activity.setMaximumComputationalUnits(2);
    activity.setMinimumComputationalUnits(1);
    activity.setTemplateUrl(templateUrl);

    return activity;
  }

  public Injector getInjector() {
    return injector;
  }

  public Injector getChildInjector() {
    return childInjector;
  }

  public void setChildInjector(Injector injector) {
    this.childInjector = injector;
  }

  public Injector whichInjector() {
    if(childInjector != null) {
      return childInjector;
    }
    return injector;
  }


}
