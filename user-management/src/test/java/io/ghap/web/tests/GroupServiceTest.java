package io.ghap.web.tests;

import com.google.inject.Inject;
import governator.junit.GovernatorJunit4Runner;
import governator.junit.config.LifecycleInjectorParams;
import io.ghap.user.form.GroupFormData;
import io.ghap.user.manager.GroupService;
import io.ghap.user.model.Group;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import javax.ws.rs.WebApplicationException;

import static io.ghap.web.tests.user.TestGroupDao.*;
import static org.junit.Assert.assertEquals;

@RunWith(GovernatorJunit4Runner.class)
@LifecycleInjectorParams(modules = UserManagementModule.class, bootstrapModule = Bootstrap.class, scannedPackages = "io.ghap.web.tests")
public class GroupServiceTest {


    @Inject
    GroupService groupService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void testGet() throws Exception {
        Group group = groupService.get("1");
        assertEquals(group.getDn(), "1");
        //assertThat(group.getParentDn(), is(notNullValue()));
    }

    @Test
    public void testCreateSuccess() throws Exception {
        GroupFormData data = new GroupFormData();
        Group group = groupService.create(data);
        assertEquals(group.getDn(), NEW_GROUP_DN);
    }

    @Test
    public void testUpdateFail() throws Exception {
        thrown.expect(WebApplicationException.class);

        groupService.update(NOT_EXISTED_GROUP_DN, INVALID_GROUP_DATA);
    }
}
