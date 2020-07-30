package io.ghap.web.tests.user;

import com.netflix.governator.annotations.AutoBindSingleton;
import io.ghap.user.dao.UserStorageDao;
import io.ghap.user.model.User;

/**
 * Created by arao on 3/7/16.
 */
@AutoBindSingleton(UserStorageDao.class)
public class TestUserStorageDao implements UserStorageDao {
  @Override
  public void createUserWorkspaceStorage(User user) {

  }

  @Override
  public void createUserLinuxHomeStorage(User user) {

  }
}
