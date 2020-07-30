package io.ghap.provision.vpg.data;

import com.google.inject.Singleton;
import io.ghap.provision.vpg.PersonalStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
public class PersonalStorageFactoryMockImpl implements PersonalStorageFactory {

  private List<PersonalStorage> storageList = new ArrayList<>();

  @Override
  public PersonalStorage create(UUID uuid, int g_size) {
    PersonalStorage storage = new PersonalStorage();
    storage.setId(UUID.randomUUID());
    storage.setSize(g_size);
    storage.setAvailabilityZone("us-east-1b");
    storage.setVolumeId("vol-12345");
    storage.setUserId(uuid);
    storageList.add(storage);

    return storage;
  }

  @Override
  public PersonalStorage get(UUID uuid) {
    for(PersonalStorage storage: storageList) {
      if(storage.getUserId().equals(uuid)) {
        return storage;
      }
    }
    return null;
  }

  @Override
  public List<PersonalStorage> get() {
    return storageList;
  }

  @Override
  public boolean available(UUID uuid) {
    return exists(uuid);
  }

  @Override
  public String state(UUID uuid) {
    return "available";
  }

  @Override
  public void detach(UUID uuid) {

  }

  @Override
  public boolean exists(UUID uuid) {
    return get(uuid) != null;
  }

  @Override
  public void delete(UUID uuid) {
    delete(get(uuid));
  }

  @Override
  public void delete(PersonalStorage storage) {
    storageList.remove(storage);
  }

  public void cleanup() {
    storageList = null;
  }

  public void setup() {
    storageList = new ArrayList<>();
  }
}
