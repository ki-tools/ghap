package io.ghap.provision.vpg.data;

import io.ghap.provision.vpg.PersonalStorage;

import java.util.List;
import java.util.UUID;

public interface PersonalStorageFactory
{
  PersonalStorage create(UUID uuid, int g_size);
  PersonalStorage get(UUID uuid);
  List<PersonalStorage> get();

  boolean available(UUID uuid);
  String state(UUID uuid);
  void detach(UUID uuid);

  boolean exists(UUID uuid);
  void delete(UUID uuid);
  void delete(PersonalStorage storage);
}
