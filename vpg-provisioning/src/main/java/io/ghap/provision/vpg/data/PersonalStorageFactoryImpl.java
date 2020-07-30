package io.ghap.provision.vpg.data;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.ec2.model.*;
import io.ghap.aws.annotations.AWSApiCall;
import io.ghap.provision.vpg.PersonalStorage;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import com.netflix.governator.annotations.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersonalStorageFactoryImpl implements PersonalStorageFactory {
  private Logger logger = LoggerFactory.getLogger(PersonalStorageFactoryImpl.class);

  @Configuration("availability.zone")
  private String availabilityZone;

  @Configuration("snapshot.id")
  private String snapshot;

  @Inject
  private Provider<EntityManager> emProvider;

  @Override
  @Transactional
  public PersonalStorage create(UUID uuid, int g_size) {
    if (!exists(uuid)) {
      AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
      AmazonEC2Client ec2Client = new AmazonEC2Client(credentials.getCredentials());
      CreateVolumeRequest volumeRequest = new CreateVolumeRequest(g_size, availabilityZone);
      volumeRequest.setEncrypted(true);
      volumeRequest.setVolumeType(VolumeType.Gp2);
      volumeRequest.setSnapshotId(snapshot);
      CreateVolumeResult result = ec2Client.createVolume(volumeRequest);
      String volumeId = result.getVolume().getVolumeId();

      CreateTagsRequest createTags = new CreateTagsRequest();
      createTags.setResources(Arrays.asList(new String[]{volumeId}));
      Tag uuid_tag = new Tag();
      uuid_tag.setKey("UUID");
      uuid_tag.setValue(uuid.toString());

      createTags.setTags(Arrays.asList(new Tag[]{uuid_tag}));
      ec2Client.createTags(createTags);

      PersonalStorage personalVolume = new PersonalStorage();
      personalVolume.setSize(g_size);
      personalVolume.setAvailabilityZone(availabilityZone);
      personalVolume.setVolumeId(volumeId);
      personalVolume.setUserId(uuid);
      emProvider.get().persist(personalVolume);
      return personalVolume;
    }
    return get(uuid);
  }

  @Override
  public PersonalStorage get(UUID uuid) {
    TypedQuery<PersonalStorage> query = emProvider.get().createQuery("from PersonalStorage where uuid = :uuid", PersonalStorage.class);
    query.setParameter("uuid", uuid);
    PersonalStorage personalVolume = null;
    try {
      personalVolume = query.getSingleResult();
    } catch (NoResultException nre) {// don't care
    }
    return personalVolume;
  }

  @Override
  public List<PersonalStorage> get() {
    TypedQuery<PersonalStorage> query = emProvider.get().createQuery("from PersonalStorage c", PersonalStorage.class);
    List<PersonalStorage> personalVolumes = null;
    try {
      personalVolumes = query.getResultList();
    } catch (NoResultException nre) {// don't care
    }
    return personalVolumes;
  }

  @Override
  @AWSApiCall
  public boolean available(UUID uuid) {
    boolean availability = false;
    PersonalStorage storage = get(uuid);

    if (storage != null) {
      AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
      AmazonEC2Client ec2Client = new AmazonEC2Client(credentials.getCredentials());

      DescribeVolumesRequest describeVolumesRequest = new DescribeVolumesRequest();
      describeVolumesRequest.setVolumeIds(Arrays.asList(storage.getVolumeId()));
      DescribeVolumesResult describeVolumesResult = ec2Client.describeVolumes(describeVolumesRequest);

      List<Volume> volumes = describeVolumesResult.getVolumes();
      if (volumes.size() == 1) {
        if (volumes.get(0).getState().equalsIgnoreCase("available")) {
          availability = true;
        }
      }
    }
    return availability;
  }

  @Override
  public String state(UUID uuid) {
    String availability = "UNKNOWN";
    PersonalStorage storage = get(uuid);

    if (storage != null) {
      AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
      AmazonEC2Client ec2Client = new AmazonEC2Client(credentials.getCredentials());

      DescribeVolumesRequest describeVolumesRequest = new DescribeVolumesRequest();
      describeVolumesRequest.setVolumeIds(Arrays.asList(storage.getVolumeId()));
      DescribeVolumesResult describeVolumesResult = ec2Client.describeVolumes(describeVolumesRequest);

      List<Volume> volumes = describeVolumesResult.getVolumes();
      if (volumes.size() == 1) {
        availability = volumes.get(0).getState();
      }
    }
    return availability;
  }

  @Override
  public void detach(UUID uuid) {
    PersonalStorage storage = get(uuid);
    if (storage != null) {
      DetachVolumeRequest detachVolumeRequest = new DetachVolumeRequest();
      detachVolumeRequest.setForce(true);
      detachVolumeRequest.setVolumeId(storage.getVolumeId());

      AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
      AmazonEC2Client ec2Client = new AmazonEC2Client(credentials.getCredentials());
      DetachVolumeResult detachVolumeResult = ec2Client.detachVolume(detachVolumeRequest);
    }
  }

  @Override
  public boolean exists(UUID uuid) {
    PersonalStorage storage = get(uuid);
    try {
      // if we have a personalstorage entry lets validate that it actually exists
      // if it doesn't really exist then nuke the entry and return false.  If it
      // does exist check it's status if the status is Ok return true otherwise
      // delete the entry and return false
      if (storage != null) {
        DescribeVolumesRequest describeVolumesRequest = new DescribeVolumesRequest();
        describeVolumesRequest.setVolumeIds(Arrays.asList(new String[]{storage.getVolumeId()}));

        AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
        AmazonEC2Client ec2Client = new AmazonEC2Client(credentials.getCredentials());
        DescribeVolumesResult volumeDescription = ec2Client.describeVolumes(describeVolumesRequest);
        if (volumeDescription.getVolumes().size() == 1) {
          DescribeVolumeStatusRequest describeVolumeStatusRequest = new DescribeVolumeStatusRequest();
          describeVolumeStatusRequest.setVolumeIds(Arrays.asList(new String[]{storage.getVolumeId()}));
          DescribeVolumeStatusResult volumeStatus = ec2Client.describeVolumeStatus(describeVolumeStatusRequest);

          List<VolumeStatusItem> volumeStatuses = volumeStatus.getVolumeStatuses();
          VolumeStatusItem status = volumeStatuses.get(0);
          VolumeStatusInfoStatus volumeStatusInfoStatus = VolumeStatusInfoStatus.fromValue(status.getVolumeStatus().getStatus());
          if (volumeStatusInfoStatus == VolumeStatusInfoStatus.Ok) {
            return true;
          }
        } else {
          emProvider.get().remove(storage);
        }
      }
      return false;
    } catch (Exception e) {
      if (logger.isDebugEnabled()) {
        logger.debug(e.getMessage(), e);
      }
      return false;
    }
  }

  @Override
  @Transactional
  public void delete(UUID uuid) {
    PersonalStorage volume = get(uuid);
    delete(volume);
  }

  @Override
  @Transactional
  public void delete(PersonalStorage storage) {
    deleteVolume(storage);
    emProvider.get().remove(storage);
  }

  @AWSApiCall
  void deleteVolume(PersonalStorage storage) {
    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonEC2Client ec2Client = new AmazonEC2Client(credentials.getCredentials());

    DeleteVolumeRequest deleteRequest = new DeleteVolumeRequest(storage.getVolumeId());

    ec2Client.deleteVolume(deleteRequest);
  }
}
