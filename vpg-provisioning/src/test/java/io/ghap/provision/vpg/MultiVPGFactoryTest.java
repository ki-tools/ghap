package io.ghap.provision.vpg;

import com.amazonaws.AmazonServiceException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.netflix.governator.LifecycleManager;
import com.netflix.governator.annotations.Configuration;
import governator.junit.config.LifecycleInjectorParams;
import io.ghap.Injectable;
import io.ghap.guice.UnitTestTrackerModule;
import io.ghap.provision.vpg.data.*;
import io.ghap.test.GovernatorParallelRunner;
import io.ghap.test.MethodModules;
import io.ghap.test.database.SetupTestDatabase;
import io.ghap.provision.vpg.guice.TestModule;
import io.ghap.provision.vpg.guice.VPGBootstrapModule;
import io.ghap.test.infrastructure.Infrastructure;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.RollbackException;
import java.util.List;
import java.util.UUID;

@Ignore
@RunWith(GovernatorParallelRunner.class)
@LifecycleInjectorParams(modules = {UnitTestTrackerModule.class}, bootstrapModule = VPGBootstrapModule.class, scannedPackages = "io.ghap")
public class MultiVPGFactoryTest implements Injectable
{
  private Logger logger = LoggerFactory.getLogger(MultiVPGFactoryTest.class);

  private static final String TEMPLATE_ANALYSIS_VPG_ACTIVITY = "https://s3.amazonaws.com/ghap-provisioning-templates-us-east-1-samba/ghap-linux-vpg-autoscale-v2.01-SAMBA.json";
  private static final String TEMPLATE_ANALYSIS_LINUX_ACTIVITY = "https://s3.amazonaws.com/ghap-provisioning-templates-us-east-1-samba/ghap-linux-host-v2.01-SAMBA.json";
  private static final String TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E = "https://s3.amazonaws.com/ghap-provisioning-templates-us-east-1-samba/ghap-windows-analysis-v1.8.3-SAMBA.json";

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

  @SetupTestDatabase(stackPrefix = "TestGetByUUID", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestGetByUUID()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);
    UUID user_uuid = UUID.randomUUID();
    List<VirtualPrivateGrid> virtual_envs = vpgFactory.get(user_uuid);

    Assert.assertEquals(0, virtual_envs.size());
  }

  @SetupTestDatabase(stackPrefix = "TestGet", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestGet()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);
    List<VirtualPrivateGrid> virtual_envs = vpgFactory.get();

    Assert.assertEquals(0, virtual_envs.size());
  }

  @SetupTestDatabase(stackPrefix = "TestCreate", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestCreate()
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

    vpgFactory.delete(user_uuid, activity.getId());
    virtual_envs = vpgFactory.get();
    Assert.assertEquals(0, virtual_envs.size());

    waitForStorage(user_uuid);
    removePersonalStorage(user_uuid);
  }

//  @SetupTestDatabase(stackPrefix = "TestCreateFailure", persistenceUnit = "vpg-provisioning")
//  @MethodModules(modules={TestModule.class})
//  @Test
  public void TestCreateFailure()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();

    createPersonalStorage(user_uuid, 1);
    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    VirtualPrivateGrid firstVpg = vpgFactory.create("test-user", "test-user@nowhere.com", user_uuid, activity);
    waitForGridToTransitionToACreatedState(user_uuid, activity);
    Assert.assertNotNull(firstVpg);

    boolean wasSecondCreationAttemptSuccessful = false;
    try {
      VirtualPrivateGrid secondVpg = vpgFactory.create("test-user", "test-user@nowhere.com", user_uuid, activity);
      wasSecondCreationAttemptSuccessful = (secondVpg != null);
    } catch(UnsupportedOperationException|RollbackException uoe) {
      //Expect second creation attempt to fail
      wasSecondCreationAttemptSuccessful = false;
    }

    //Ensure the grid is deleted (regardless of success or failure of second creation attempt)
    //This ensure that the created grid is cleaned up
    vpgFactory.delete(user_uuid, activity.getId());
    waitForStorage(user_uuid);
    removePersonalStorage(user_uuid);
    Assert.assertFalse("Did not expect duplicate attempt to create grid to be successful.", wasSecondCreationAttemptSuccessful);

    Assert.assertFalse(vpgFactory.exists(user_uuid, activity));
  }

  @SetupTestDatabase(stackPrefix = "TestCreateMultipleSingleRequests", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestCreateMultipleSingleRequests()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();

    Activity activity_one = createActivity("Linux", TEMPLATE_ANALYSIS_VPG_ACTIVITY);
    Activity activity_two = createActivity("Windows 1", TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E);
    Activity activity_three = createActivity("Windows 2", TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E);
    Activity activity_four = createActivity("Windows 3", TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E);

    Activity[] activities = new Activity[] { activity_one, activity_two, activity_three, activity_four };

    createPersonalStorage(user_uuid, 1);

    vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activities);

    List<VirtualPrivateGrid> virtual_envs = vpgFactory.get();
    Assert.assertEquals(4, virtual_envs.size());

    for(Activity activity : activities) {
      vpgFactory.delete(user_uuid, activity.getId());
    }

    virtual_envs = vpgFactory.get();
    Assert.assertEquals(0, virtual_envs.size());

    waitForStorage(user_uuid);
    removePersonalStorage(user_uuid);
  }

  @SetupTestDatabase(stackPrefix = "TestCreateMultipleSeparateRequests", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestCreateMultipleSeparateRequests()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();

    createPersonalStorage(user_uuid, 1);

    Activity activity_one = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);
    Activity activity_two = createActivity("Test", TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E);

    VirtualPrivateGrid vpg_one = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activity_one);
    Assert.assertNotNull(vpg_one);

    VirtualPrivateGrid vpg_two = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activity_two);
    Assert.assertNotNull(vpg_two);

    long start = System.currentTimeMillis();
    for(long duration = 0; duration < TIMEOUT; duration = System.currentTimeMillis() - start)
    {
      ActivityStatus[] activityStatuses = vpgFactory.statuses(user_uuid, new Activity[] { activity_one, activity_two});
      Assert.assertEquals(2, activityStatuses.length);

      boolean completed = true;
      for(ActivityStatus status: activityStatuses) {
        completed = completed && status.getStatus().endsWith("_COMPLETE");
      }

      if(completed) {
        break;
      }
    }

    List<VirtualPrivateGrid> virtual_envs = vpgFactory.get();
    Assert.assertEquals(2, virtual_envs.size());

    vpgFactory.delete(user_uuid, activity_one.getId());

    virtual_envs = vpgFactory.get();
    Assert.assertEquals(1, virtual_envs.size());

    vpgFactory.delete(user_uuid, activity_two.getId());

    virtual_envs = vpgFactory.get();
    Assert.assertEquals(0, virtual_envs.size());

    waitForStorage(user_uuid);
    removePersonalStorage(user_uuid);
  }

  @SetupTestDatabase(stackPrefix = "TestExists", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestExists()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();
    createPersonalStorage(user_uuid, 1);
    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    Assert.assertFalse(vpgFactory.exists(user_uuid, activity));

    VirtualPrivateGrid vpg = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activity);
    waitForGridToTransitionToACreatedState(user_uuid, activity);
    Assert.assertNotNull(vpg);

    waitForGridToTransitionToACreatedState(user_uuid, activity);

    Assert.assertTrue(vpgFactory.exists(user_uuid, activity));

    vpgFactory.delete(user_uuid, activity.getId());
    waitForStorage(user_uuid);
    removePersonalStorage(user_uuid);
    Assert.assertFalse(vpgFactory.exists(user_uuid, activity));
  }

  @SetupTestDatabase(stackPrefix = "TestExistenceOfMultipleActivities", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestExistenceOfMultipleActivities()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();

    createPersonalStorage(user_uuid, 1);

    Activity activity_one = createActivity("Linux", TEMPLATE_ANALYSIS_VPG_ACTIVITY);
    Activity activity_two = createActivity("Windows 1", TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E);

    Activity[] activities = new Activity[]{ activity_one, activity_two};

    VirtualPrivateGrid[] vpgs = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activities);
    waitForGridToTransitionToACreatedState(user_uuid, activity_one);
    waitForGridToTransitionToACreatedState(user_uuid, activity_two);
    Assert.assertNotNull(vpgs);

    ActivityExistence[] existences = vpgFactory.existences(user_uuid, activities);
    Assert.assertEquals("Unexpected activities exist", activities.length, existences.length);

    for (ActivityExistence existence : existences) {
      Assert.assertTrue(
              String.format("Expected activity <%s> to exist", existence.getActivityId()),
              existence.getExistence());
    }

    for(Activity activity : activities) {
      vpgFactory.delete(user_uuid, activity.getId());
    }

    existences = vpgFactory.existences(user_uuid, activities);
    Assert.assertEquals("Unexpected activities exist", activities.length, existences.length);

    for (ActivityExistence existence : existences) {
      Assert.assertFalse(
              String.format("Did not expect activity <%s> to exist", existence.getActivityId()),
              existence.getExistence());
    }

    waitForStorage(user_uuid);
    removePersonalStorage(user_uuid);
  }

  @SetupTestDatabase(stackPrefix = "TestGetVirtualGridFromStackIdentifier", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestGetVirtualGridFromStackIdentifier()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();

    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    Assert.assertFalse(vpgFactory.exists(user_uuid, activity));

    VirtualPrivateGrid vpg = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activity);
    Assert.assertNotNull(vpg);

    VirtualPrivateGrid vpgFromStackId = vpgFactory.getByStackId(vpg.getStackId());
    Assert.assertNotNull("Expected a grid to be found", vpgFromStackId);
    Assert.assertEquals("Unexpected grid found using the stack id", vpg.getId(), vpgFromStackId.getId());

    vpgFactory.delete(user_uuid, activity.getId());

    Assert.assertFalse(vpgFactory.exists(user_uuid, activity));
  }

  @SetupTestDatabase(stackPrefix = "TestMultipleUsers", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestMultipleUsers()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid_one = UUID.randomUUID();

    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    Assert.assertFalse(vpgFactory.exists(user_uuid_one, activity));

    VirtualPrivateGrid vpg_user_one = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid_one, activity);
    Assert.assertNotNull(vpg_user_one);

    Assert.assertTrue(vpgFactory.exists(user_uuid_one, activity));

    UUID user_uuid_two = UUID.randomUUID();
    VirtualPrivateGrid vpg_user_two = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid_two, activity);
    Assert.assertNotNull(vpg_user_two);

    Assert.assertTrue(vpgFactory.exists(user_uuid_two, activity));

    List<VirtualPrivateGrid> vpgs = vpgFactory.get();
    Assert.assertEquals(2, vpgs.size());

    List<VirtualPrivateGrid> vpgs_user_one = vpgFactory.get(user_uuid_one);
    Assert.assertEquals(1, vpgs_user_one.size());

    List<VirtualPrivateGrid> vpgs_user_two = vpgFactory.get(user_uuid_two);
    Assert.assertEquals(1, vpgs_user_two.size());

    Assert.assertNotEquals(vpgs_user_one.get(0).getId(), vpgs_user_two.get(0).getId());

    vpgFactory.delete(user_uuid_one, activity.getId());
    vpgFactory.delete(user_uuid_two, activity.getId());

    Assert.assertFalse(vpgFactory.exists(user_uuid_two, activity));
    Assert.assertFalse(vpgFactory.exists(user_uuid_one, activity));
  }

  @SetupTestDatabase(stackPrefix = "TestMultiUserMultiActivity", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestMultiUserMultiActivity()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid_one = UUID.randomUUID();

    Activity activity_one = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    Activity activity_two = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);


    Assert.assertFalse(vpgFactory.exists(user_uuid_one, activity_one));

    VirtualPrivateGrid vpg_user_one_env_one = vpgFactory.create("test-user-one", "test-user@nowhere.com",user_uuid_one, activity_one);
    VirtualPrivateGrid vpg_user_one_env_two = vpgFactory.create("test-user-one", "test-user@nowhere.com",user_uuid_one, activity_two);
    Assert.assertNotNull(vpg_user_one_env_one);
    Assert.assertNotNull(vpg_user_one_env_two);

    Assert.assertTrue(vpgFactory.exists(user_uuid_one, activity_one));
    Assert.assertTrue(vpgFactory.exists(user_uuid_one, activity_two));

    UUID user_uuid_two = UUID.randomUUID();
    VirtualPrivateGrid vpg_user_two_env_one = vpgFactory.create("test-user-two", "test-user@nowhere.com",user_uuid_two, activity_one);
    VirtualPrivateGrid vpg_user_two_env_two = vpgFactory.create("test-user-two", "test-user@nowhere.com",user_uuid_two, activity_two);

    Assert.assertNotNull(vpg_user_two_env_one);
    Assert.assertNotNull(vpg_user_two_env_two);

    Assert.assertTrue(vpgFactory.exists(user_uuid_two, activity_one));
    Assert.assertTrue(vpgFactory.exists(user_uuid_two, activity_two));

    List<VirtualPrivateGrid> vpgs = vpgFactory.get();
    Assert.assertEquals(4, vpgs.size());

    List<VirtualPrivateGrid> vpgs_user_one = vpgFactory.get(user_uuid_one);
    Assert.assertEquals(2, vpgs_user_one.size());

    List<VirtualPrivateGrid> vpgs_user_two = vpgFactory.get(user_uuid_two);
    Assert.assertEquals(2, vpgs_user_two.size());

    Assert.assertNotEquals(vpgs_user_one.get(0).getId(), vpgs_user_two.get(0).getId());
    Assert.assertNotEquals(vpgs_user_one.get(1).getId(), vpgs_user_two.get(1).getId());
    Assert.assertNotEquals(vpgs_user_one.get(0).getId(), vpgs_user_one.get(1).getId());
    Assert.assertNotEquals(vpgs_user_two.get(0).getId(), vpgs_user_two.get(1).getId());
    Assert.assertNotEquals(vpgs_user_one.get(1).getId(), vpgs_user_two.get(0).getId());
    Assert.assertNotEquals(vpgs_user_one.get(0).getId(), vpgs_user_two.get(1).getId());

    vpgFactory.delete(user_uuid_one, activity_one.getId());
    vpgFactory.delete(user_uuid_one, activity_two.getId());
    vpgFactory.delete(user_uuid_two, activity_one.getId());
    vpgFactory.delete(user_uuid_two, activity_two.getId());

    Assert.assertFalse(vpgFactory.exists(user_uuid_two, activity_one));
    Assert.assertFalse(vpgFactory.exists(user_uuid_one, activity_one));
    Assert.assertFalse(vpgFactory.exists(user_uuid_two, activity_two));
    Assert.assertFalse(vpgFactory.exists(user_uuid_one, activity_two));
  }

  @SetupTestDatabase(stackPrefix = "TestGetByVpgId", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestGetByVpgId()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();

    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    VirtualPrivateGrid vpg_one = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activity);
    Assert.assertNotNull(vpg_one);

    Assert.assertNotNull(vpg_one.getStackId());
    Assert.assertNotNull(vpg_one.getActivityId());
    Assert.assertNotNull(vpg_one.getId());
    Assert.assertNotNull(vpg_one.getUserId());
    Assert.assertNotNull(vpg_one.getPemKey());

    VirtualPrivateGrid vpg_two = vpgFactory.getByVpgId(vpg_one.getId());

    Assert.assertEquals(vpg_one.getStackId(),vpg_two.getStackId());
    Assert.assertEquals(vpg_one.getActivityId(), vpg_two.getActivityId());
    Assert.assertEquals(vpg_one.getId(), vpg_two.getId());
    Assert.assertEquals(vpg_one.getUserId(), vpg_two.getUserId());
    Assert.assertEquals(vpg_one.getPemKey(), vpg_two.getPemKey());

    vpgFactory.delete(user_uuid, activity.getId());
  }

  @SetupTestDatabase(stackPrefix = "TestStatus", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestStatus()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();
    PersonalStorage storage = createPersonalStorage(user_uuid, 1);

    PersonalStorageFactory personalStorageFactory = whichInjector().getInstance(PersonalStorageFactory.class);
    Assert.assertNotNull(storage);
    Assert.assertTrue(personalStorageFactory.exists(user_uuid));

    waitForStorage(user_uuid);

    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    VirtualPrivateGrid vpg_one = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activity);
    Assert.assertNotNull(vpg_one);

    waitForGridToTransitionToACreatedState(user_uuid, activity);

    Assert.assertEquals("CREATE_COMPLETE", vpgFactory.status(user_uuid, activity));
    vpgFactory.delete(user_uuid, activity.getId());

    personalStorageFactory.detach(user_uuid);
    waitForStorage(user_uuid);

    removePersonalStorage(user_uuid);

    Assert.assertFalse(personalStorageFactory.exists(user_uuid));
    Assert.assertFalse(vpgFactory.exists(user_uuid, activity));
  }

  @SetupTestDatabase(stackPrefix = "TestPause", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestPause()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);
    PersonalStorageFactory personalStorageFactory = whichInjector().getInstance(PersonalStorageFactoryImpl.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();

    createPersonalStorage(user_uuid, 1);

    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    VirtualPrivateGrid vpg = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activity);
    Assert.assertNotNull(vpg);

    waitForGridToTransitionToACreatedState(user_uuid, activity);
    assertThatGridResourcesAreCreated(user_uuid, activity);


    vpgFactory.pause(user_uuid, activity);

    waitForGridToTransitionToAStoppedState(vpg);
    assertThatGridResourcesAreStopped(vpg);

    vpgFactory.delete(user_uuid, activity.getId());

    removePersonalStorage(user_uuid);

    Assert.assertFalse(personalStorageFactory.exists(user_uuid));
    Assert.assertFalse(vpgFactory.exists(user_uuid, activity));
  }

  @SetupTestDatabase(stackPrefix = "TestResume", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestResume()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);
    PersonalStorageFactory personalStorageFactory = whichInjector().getInstance(PersonalStorageFactoryImpl.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();

    createPersonalStorage(user_uuid, 1);

    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    VirtualPrivateGrid vpg = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activity);
    Assert.assertNotNull(vpg);

    waitForGridToTransitionToACreatedState(user_uuid, activity);
    assertThatGridResourcesAreCreated(user_uuid, activity);

    vpgFactory.pause(user_uuid, activity);

    waitForGridToTransitionToAStoppedState(vpg);
    assertThatGridResourcesAreStopped(vpg);

    // start it back up
    vpgFactory.resume(user_uuid, activity);

    waitForGridToTransitionToARunningState(vpg);

    assertThatGridResourcesAreRunning(vpg);


    vpgFactory.delete(user_uuid, activity.getId());

    removePersonalStorage(user_uuid);

    Assert.assertFalse(personalStorageFactory.exists(user_uuid));
    Assert.assertFalse(vpgFactory.exists(user_uuid, activity));
  }

  @SetupTestDatabase(stackPrefix = "TestConsole", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestConsole()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);
    PersonalStorageFactory personalStorageFactory = whichInjector().getInstance(PersonalStorageFactoryImpl.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();
    PersonalStorage storage =createPersonalStorage(user_uuid, 1);

    Assert.assertNotNull(storage);
    Assert.assertTrue(personalStorageFactory.exists(user_uuid));

    waitForStorage(user_uuid);

    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_VPG_ACTIVITY);

    VirtualPrivateGrid vpg = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activity);
    Assert.assertNotNull(vpg);
    waitForGridToTransitionToACreatedState(user_uuid, activity);
    assertThatGridResourcesAreCreated(user_uuid, activity);
    List<VirtualResource> resources = vpgFactory.getVirtualResources(vpg);
    Assert.assertTrue(resources.size() > 0);

    for(VirtualResource resource : resources) {
      String console = vpgFactory.console(resource);
      Assert.assertNotNull(console);
    }
    vpgFactory.delete(user_uuid, activity.getId());
    personalStorageFactory.detach(user_uuid);
    waitForStorage(user_uuid);
    removePersonalStorage(user_uuid);

    Assert.assertFalse(personalStorageFactory.exists(user_uuid));
    Assert.assertFalse(vpgFactory.exists(user_uuid, activity));
  }

  @SetupTestDatabase(stackPrefix = "TestResourceStatus", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestResourceStatus()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();

    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E);

    VirtualPrivateGrid vpg = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activity);
    Assert.assertNotNull(vpg);

    waitForGridToTransitionToACreatedState(user_uuid, activity);
    Assert.assertEquals(1, vpgFactory.getVirtualResources(vpg).size());

    long start = System.currentTimeMillis();
    long sleep_time = 1;

    for(long duration = 0; duration <= (TIMEOUT * 3); duration = System.currentTimeMillis() - start) {
      try {
        List<VirtualResource> resources = vpgFactory.getVirtualResources(vpg);
        boolean running = true;
        for (VirtualResource resource : resources) {
          Assert.assertNotEquals("impaired", resource.getStatus().toLowerCase());
          Assert.assertNotEquals("ok", resource.getStatus().toLowerCase());
          running = running && resource.getStatus().toLowerCase().equals("running");
        }
        if (running) { // if everything is in the running state bail out of this loop
          break;
        }
        try {
          Thread.sleep(sleep_time);
        } catch (InterruptedException ie) {
        }
      }
      catch(AmazonServiceException ase)
      {
        if(ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling"))
        {
          sleep_time=sleep_time*2;
          continue;
        }
        throw ase;
      }
    }
    vpgFactory.delete(user_uuid, activity.getId());
    Assert.assertFalse(vpgFactory.exists(user_uuid, activity));
  }

  @SetupTestDatabase(stackPrefix = "TestRdpByVirtualResource", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestRdpByVirtualResource()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();

    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E);

    VirtualPrivateGrid vpg = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activity);
    Assert.assertNotNull(vpg);

    waitForGridToTransitionToACreatedState(user_uuid, activity);

    long start = System.currentTimeMillis();
    long sleep_time = 1;

    for(long duration = 0; duration < TIMEOUT; duration = System.currentTimeMillis() - start) {
      try
      {
        List<VirtualResource> resources = vpgFactory.getVirtualResources(vpg);
        for(VirtualResource resource: resources) {
          Assert.assertNotEquals("impaired", resource.getStatus().toLowerCase());
        }
      }
      catch(AmazonServiceException ase)
      {
        if(ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling"))
        {
          sleep_time*=2;
          continue;
        }
        break;
      }
      try { Thread.sleep(sleep_time);} catch(InterruptedException ie) {}
    }

    String username = "test-user";
    VirtualResource resource = vpgFactory.getVirtualResources(vpg).get(0);
    String rdpContent = vpgFactory.rdp(resource, username);

    vpgFactory.delete(user_uuid, activity.getId());
    Assert.assertFalse(vpgFactory.exists(user_uuid, activity));
    Assert.assertNotNull(rdpContent);
    Assert.assertTrue(rdpContent.length() > 0);
    String rdp = String.format(
        "auto connect:i:1\nfull address:s:%s\n" +
            "prompt for credentials on client:i:1\nusername:s:PROD\\%s",
        resource.getAddress(),
        username);
    Assert.assertEquals(rdp, rdpContent);
  }

  @SetupTestDatabase(stackPrefix = "TestGetVirtualResourcesByUser", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestGetVirtualResourcesByUser()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();

    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E);

    VirtualPrivateGrid vpg = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activity);
    Assert.assertNotNull(vpg);

    waitForGridToTransitionToACreatedState(user_uuid, activity);

    long start = System.currentTimeMillis();
    long sleep_time = 1;

    for(long duration = 0; duration < TIMEOUT; duration = System.currentTimeMillis() - start) {
      try
      {
        List<VirtualResource> resources = vpgFactory.getVirtualResources(user_uuid);
        for(VirtualResource resource: resources) {
          Assert.assertNotEquals("impaired", resource.getStatus().toLowerCase());
        }
      }
      catch(AmazonServiceException ase)
      {
        if(ase.getStatusCode() == 400 && ase.getErrorCode().equals("Throttling"))
        {
          sleep_time*=2;
          continue;
        }
        break;
      }
      try { Thread.sleep(sleep_time);} catch(InterruptedException ie) {}
    }

    String username = "test-user";
    VirtualResource resource = vpgFactory.getVirtualResources(user_uuid).get(0);
    Assert.assertNotNull(resource);


    vpgFactory.delete(user_uuid, activity.getId());
    Assert.assertFalse(vpgFactory.exists(user_uuid, activity));

  }

  @SetupTestDatabase(stackPrefix = "TestRdpByAddress", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestRdpByAddress() {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();

    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_WINDOW_ACTIVITY_TEST_V8E);

    VirtualPrivateGrid vpg = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activity);
    Assert.assertNotNull(vpg);

    waitForGridToTransitionToACreatedState(user_uuid, activity);
    String username = "test-user";
    VirtualResource resource = vpgFactory.getVirtualResources(vpg).get(0);
    String rdpContent = vpgFactory.rdp(resource.getAddress(), username);

    vpgFactory.delete(user_uuid, activity.getId());
    Assert.assertFalse(vpgFactory.exists(user_uuid, activity));
    Assert.assertNotNull(rdpContent);
    Assert.assertTrue(rdpContent.length() > 0);
    String rdp = String.format(
        "auto connect:i:1\nfull address:s:%s\n" +
            "prompt for credentials on client:i:1\nusername:s:PROD\\%s",
        resource.getAddress(),
        username);
    Assert.assertEquals(rdp, rdpContent);
  }

//  @SetupTestDatabase(stackPrefix = "TestScheduleStopForIdleResources", persistenceUnit = "vpg-provisioning")
//  @MethodModules(modules={TestModule.class})
//  @Test
  public void TestScheduleStopForIdleResources()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);
    PersonalStorageFactory personalStorageFactory = whichInjector().getInstance(PersonalStorageFactoryImpl.class);

      Assert.assertNotNull(vpgFactory);
      Assert.assertTrue(vpgFactory.get().size() == 0);

      UUID user_uuid = UUID.randomUUID();

      createPersonalStorage(user_uuid, 1);

      Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_LINUX_ACTIVITY);

      VirtualPrivateGrid vpg = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activity);
      Assert.assertNotNull(vpg);

      waitForGridToTransitionToACreatedState(user_uuid, activity);
      assertThatGridResourcesAreCreated(user_uuid, activity);


      vpgFactory.scheduleStopForIdleProvisionedResources(vpg);


      try {
        Thread.sleep(30 * 1000);
      } catch (InterruptedException ignore) {}


      waitForGridToTransitionToAStoppedState(vpg);
      assertThatGridResourcesAreStopped(vpg);


      vpgFactory.delete(user_uuid, activity.getId());

      removePersonalStorage(user_uuid);

      Assert.assertFalse(personalStorageFactory.exists(user_uuid));
      Assert.assertFalse(vpgFactory.exists(user_uuid, activity));
    }

  @SetupTestDatabase(stackPrefix = "TestCancelScheduleStopForIdleResources", persistenceUnit = "vpg-provisioning")
  @MethodModules(modules={TestModule.class})
  @Test
  public void TestCancelScheduleStopForIdleResources()
  {
    VPGMultiFactory vpgFactory = whichInjector().getInstance(VPGMultiFactory.class);
    PersonalStorageFactory personalStorageFactory = whichInjector().getInstance(PersonalStorageFactoryImpl.class);

    Assert.assertNotNull(vpgFactory);
    Assert.assertTrue(vpgFactory.get().size() == 0);

    UUID user_uuid = UUID.randomUUID();

    createPersonalStorage(user_uuid, 1);

    Activity activity = createActivity("Test", TEMPLATE_ANALYSIS_LINUX_ACTIVITY);

    VirtualPrivateGrid vpg = vpgFactory.create("test-user", "test-user@nowhere.com",user_uuid, activity);
    Assert.assertNotNull(vpg);

    waitForGridToTransitionToACreatedState(user_uuid, activity);
    assertThatGridResourcesAreCreated(user_uuid, activity);

    waitForGridToTransitionToARunningState(vpg);
    assertThatGridResourcesAreRunning(vpg);

    vpgFactory.scheduleStopForIdleProvisionedResources(vpg);

    //For the test-cases, the job is scheduled to be kicked off in 15 seconds, so lets wait for 5 seconds and then
    //cancel the scheduled stop.
    try {
      Thread.sleep(5 * 1000);
    } catch (InterruptedException ignore) {}


    vpgFactory.cancelScheduledStopForIdleProvisionedResources(vpg);

    try {
      Thread.sleep(30 * 1000);
    } catch (InterruptedException ignore) {}


    assertThatGridResourcesAreRunning(vpg);


    vpgFactory.delete(user_uuid, activity.getId());

    removePersonalStorage(user_uuid);

    Assert.assertFalse(personalStorageFactory.exists(user_uuid));
    Assert.assertFalse(vpgFactory.exists(user_uuid, activity));
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
