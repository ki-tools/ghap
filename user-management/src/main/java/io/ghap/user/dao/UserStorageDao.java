package io.ghap.user.dao;

import io.ghap.user.model.User;

/**
 * Created by arao on 3/7/16.
 */
public interface UserStorageDao {
  void createUserWorkspaceStorage(User user);
  void createUserLinuxHomeStorage(User user);

}
