package io.ghap.reporting.service;

import io.ghap.reporting.data.User;
import io.ghap.reporting.service.impl.UserServiceBean;

import java.util.List;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
public interface UserService {
    List<User> loadUsers(String accessToken);

    List<UserServiceBean.GroupWithUsers> loadGroupsWithUsers(String accessToken);

    List<UserServiceBean.GroupWithUsers> loadRolesWithUsers(String accessToken);
}
