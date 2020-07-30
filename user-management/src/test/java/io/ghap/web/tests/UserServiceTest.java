package io.ghap.web.tests;

import com.google.inject.Inject;
import governator.junit.GovernatorJunit4Runner;
import governator.junit.config.LifecycleInjectorParams;
import io.ghap.user.form.UserFormData;
import io.ghap.user.manager.UserService;
import io.ghap.user.model.User;
import io.ghap.web.tests.auth.TestSecurityContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import javax.ws.rs.WebApplicationException;

import static io.ghap.web.tests.user.TestUserDao.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(GovernatorJunit4Runner.class)
@LifecycleInjectorParams(modules = UserManagementModule.class, bootstrapModule = Bootstrap.class, scannedPackages = "io.ghap.web.tests")
public class UserServiceTest {


    @Inject
    UserService userService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConfigurationBinding(){
        assertThat(userService.getServiceName(), equalTo("Test User Service"));
    }

    @Test
    public void testGet() throws Exception {
        User user = userService.get("1");
        assertEquals(user.getDn(), "1");
        //assertThat(user.getParentDn(), is(notNullValue()));
    }

    @Test
    public void testCreateSuccess() throws Exception {
        UserFormData data = new UserFormData();
        data.setPassword("123");
        User user = userService.create(data);
        assertEquals(user.getDn(), NEW_USER_DN);
    }


    @Test
    public void testUpdateFail() throws Exception {
        thrown.expect(WebApplicationException.class);

        userService.update(NOT_EXISTED_USER_DN, INVALID_USER_DATA, new TestSecurityContext());
    }
}
