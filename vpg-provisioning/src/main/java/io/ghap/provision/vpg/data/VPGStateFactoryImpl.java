package io.ghap.provision.vpg.data;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import io.ghap.provision.vpg.VirtualPrivateGridStateEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class VPGStateFactoryImpl implements VPGStateFactory {
  private Logger logger = LoggerFactory.getLogger(VPGMultiFactoryImpl.class);

  @Inject
  private Provider<EntityManager> emProvider;

  @Override
  @Transactional
  public VirtualPrivateGridStateEntry create(UUID activity_guid,
                                             UUID user_uuid,
                                             String stackId,
                                             VirtualPrivateGridStateEntry.State state) {
    VirtualPrivateGridStateEntry vpgStateEntry =
        new VirtualPrivateGridStateEntry(activity_guid,
            user_uuid,
            stackId,
            state);
    emProvider.get().persist(vpgStateEntry);
    return vpgStateEntry;
  }

  @Override
  public List<VirtualPrivateGridStateEntry> get(UUID user_uuid) {
    TypedQuery<VirtualPrivateGridStateEntry> query = emProvider.get().createQuery("from VirtualPrivateGridStateEntry where userId = :user_uuid", VirtualPrivateGridStateEntry.class);
    query.setParameter("user_uuid", user_uuid);
    List<VirtualPrivateGridStateEntry> results = null;
    try {
      results = query.getResultList();
    } catch (NoResultException nre) {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("No result found for user %s", user_uuid));
      }
      results = new ArrayList<VirtualPrivateGridStateEntry>();
    }
    return results;
  }

  @Override
  public List<VirtualPrivateGridStateEntry> get(UUID user_uuid, UUID activity_guid)
  {
    TypedQuery<VirtualPrivateGridStateEntry> query = emProvider.get().createQuery("from VirtualPrivateGridStateEntry where userId = :user_uuid and activityId = :activity_guid", VirtualPrivateGridStateEntry.class);
    query.setParameter("user_uuid", user_uuid);
    query.setParameter("activity_guid", activity_guid);
    List<VirtualPrivateGridStateEntry> results = null;
    try {
      results = query.getResultList();
    } catch(NoResultException nre) {
      if(logger.isDebugEnabled()) {
        logger.debug(String.format("No results found for user %s and activity %s", user_uuid, activity_guid));
      }
      results = new ArrayList<VirtualPrivateGridStateEntry>();
    }
    return results;
  }

  @Override
  public List<VirtualPrivateGridStateEntry> get(String stackId)
  {
    TypedQuery<VirtualPrivateGridStateEntry> query = emProvider.get().createQuery("from VirtualPrivateGridStateEntry where stackId = :stack_id", VirtualPrivateGridStateEntry.class);
    query.setParameter("stack_id", stackId);
    List<VirtualPrivateGridStateEntry> results = null;
    try {
      results = query.getResultList();
    } catch(NoResultException nre) {
      if(logger.isDebugEnabled()) {
        logger.debug(String.format("No results found for stack %s", stackId));
      }
      results = new ArrayList<VirtualPrivateGridStateEntry>();
    }
    return results;
  }

  @Override
  public List<VirtualPrivateGridStateEntry> get(UUID user_uuid, Date start, Date end) {
    TypedQuery<VirtualPrivateGridStateEntry> query =
        emProvider.get().createQuery("from VirtualPrivateGridStateEntry where userId = :user_uuid and timestamp >= :start and timestamp <= :end",
                                      VirtualPrivateGridStateEntry.class);
    query.setParameter("user_uuid", user_uuid);
    query.setParameter("start", start);
    query.setParameter("end", end);
    List<VirtualPrivateGridStateEntry> results = null;
    try {
      results = query.getResultList();
    } catch (NoResultException nre) {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("No result found for user %s in range %s to %s", user_uuid, start, end));
      }
      results = new ArrayList<VirtualPrivateGridStateEntry>();
    }
    return results;
  }

  @Override
  public List<VirtualPrivateGridStateEntry> get(UUID user_uuid, UUID activity_guid, Date start, Date end) {
    TypedQuery<VirtualPrivateGridStateEntry> query =
        emProvider.get().createQuery("from VirtualPrivateGridStateEntry where userId = :user_uuid and activityId = :activity_guid and timestamp >= :start and timestamp <= :end",
            VirtualPrivateGridStateEntry.class);
    query.setParameter("user_uuid", user_uuid);
    query.setParameter("activity_guid", activity_guid);
    query.setParameter("start", start);
    query.setParameter("end", end);
    List<VirtualPrivateGridStateEntry> results = null;
    try {
      results = query.getResultList();
    } catch (NoResultException nre) {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("No result found for user %s with activity %s in range %s to %s", user_uuid, activity_guid, start, end));
      }
      results = new ArrayList<VirtualPrivateGridStateEntry>();
    }
    return results;
  }

  @Override
  public List<VirtualPrivateGridStateEntry> get(String stackId, Date start, Date end) {
    TypedQuery<VirtualPrivateGridStateEntry> query =
        emProvider.get().createQuery("from VirtualPrivateGridStateEntry where stackId = :stackid and timestamp >= :start and timestamp <= :end",
            VirtualPrivateGridStateEntry.class);
    query.setParameter("stackid", stackId);
    query.setParameter("start", start);
    query.setParameter("end", end);
    List<VirtualPrivateGridStateEntry> results = null;
    try {
      results = query.getResultList();
    } catch (NoResultException nre) {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("No result found for stack %s in range %s to %s", stackId, start, end));
      }
      results = new ArrayList<VirtualPrivateGridStateEntry>();
    }
    return results;
  }

  @Override
  public List<VirtualPrivateGridStateEntry> get() {
    TypedQuery<VirtualPrivateGridStateEntry> query =
        emProvider.get().createQuery("from VirtualPrivateGridStateEntry c",
            VirtualPrivateGridStateEntry.class);
    List<VirtualPrivateGridStateEntry> results = null;
    try {
      results = query.getResultList();
    } catch (NoResultException nre) {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("No result found"));
      }
      results = new ArrayList<VirtualPrivateGridStateEntry>();
    }
    return results;
  }

  @Override
  public List<String> getStackNames(Date start, Date end) {
    TypedQuery<String> query = emProvider.get().createQuery("select DISTINCT c.stackId from VirtualPrivateGridStateEntry c where timestamp >= :start and timestamp <= :end", String.class);
    query.setParameter("start", start);
    query.setParameter("end", end);
    List<String> results = null;
    try  {
      results = query.getResultList();
    } catch(NoResultException nre) {
      if(logger.isDebugEnabled()) {
        logger.debug(String.format("No distinct stack names found"));
      }
      results = new ArrayList<String>();
    }
    return results;
  }

  @Override
  public List<UUID> getUsers(Date start, Date end) {
    TypedQuery<UUID> query = emProvider.get().createQuery("select DISTINCT c.userId from VirtualPrivateGridStateEntry c where timestamp >= :start and timestamp <= :end", UUID.class);
    query.setParameter("start", start);
    query.setParameter("end", end);
    List<UUID> results = null;
    try  {
      results = query.getResultList();
    } catch(NoResultException nre) {
      if(logger.isDebugEnabled()) {
        logger.debug(String.format("No distinct user names found"));
      }
      results = new ArrayList<UUID>();
    }
    return results;
  }

  @Override
  public List<UUID> getActivities(UUID user_uuid) {
    TypedQuery<UUID> query = emProvider.get().createQuery("select DISTINCT c.activityId from VirtualPrivateGridStateEntry c where userId = :user_uuid", UUID.class);
    query.setParameter("user_uuid", user_uuid);
    List<UUID> results = null;
    try  {
      results = query.getResultList();
    } catch(NoResultException nre) {
      if(logger.isDebugEnabled()) {
        logger.debug(String.format("No distinct stack names found"));
      }
      results = new ArrayList<UUID>();
    }
    return results;
  }

  @Override
  public List<UUID> getActivities(Date start, Date end) {
    TypedQuery<UUID> query = emProvider.get().createQuery("select DISTINCT c.activityId from VirtualPrivateGridStateEntry c where timestamp >= :start and timestamp <= :end", UUID.class);
    query.setParameter("start", start);
    query.setParameter("end", end);
    List<UUID> results = null;
    try  {
      results = query.getResultList();
    } catch(NoResultException nre) {
      if(logger.isDebugEnabled()) {
        logger.debug(String.format("No distinct activities found in range %s to %s", start, end));
      }
      results = new ArrayList<UUID>();
    }
    return results;
  }

  @Override
  public List<UUID> getActivities(UUID user_uuid, Date start, Date end) {
    TypedQuery<UUID> query = emProvider.get().createQuery("select DISTINCT c.activityId from VirtualPrivateGridStateEntry c where userId = :user_uuid and timestamp >= :start and timestamp <= :end", UUID.class);
    query.setParameter("user_uuid", user_uuid);
    query.setParameter("start", start);
    query.setParameter("end", end);
    List<UUID> results = null;
    try  {
      results = query.getResultList();
    } catch(NoResultException nre) {
      if(logger.isDebugEnabled()) {
        logger.debug(String.format("No distinct activities found for user in range %s to %s", user_uuid, start, end));
      }
      results = new ArrayList<UUID>();
    }
    return results;
  }

  @Override
  /**
   * Get state change entries of the specified date range, ordered by timestamp in ascending order
   */
  public List<VirtualPrivateGridStateEntry> get(Date start, Date end)
  {
    TypedQuery<VirtualPrivateGridStateEntry> query = emProvider.get().createQuery("from VirtualPrivateGridStateEntry where timestamp >= :start and timestamp <= :end order by timestamp asc", VirtualPrivateGridStateEntry.class);
    query.setParameter("start", start);
    query.setParameter("end", end);
    List<VirtualPrivateGridStateEntry> results = null;
    try {
      results = query.getResultList();
    } catch(NoResultException nre) {
      if(logger.isDebugEnabled()) {
        logger.debug(String.format("No results found, date range %s to %s", start, end));
      }
      results = new ArrayList<VirtualPrivateGridStateEntry>();
    }
    return results;
  }

  @Override
  /**
   * Get state that was active on the specified date, for the specified stack and user. 
   * That is, get state-change event that is both closest to the specified date and happened before the specified date.
   */
  public VirtualPrivateGridStateEntry getActiveState(String stackId, UUID userId, Date limit)
  {
    logger.debug("Getting active state for stack {}, user {}, upper limit date {}", stackId, userId, limit);
    List<VirtualPrivateGridStateEntry> results = null;

    try {
      TypedQuery<VirtualPrivateGridStateEntry> query = emProvider.get().createQuery("select v FROM VirtualPrivateGridStateEntry v WHERE v.stackId = :stackId AND v.userId = :userId and v.timestamp IN (SELECT MAX(x.timestamp) FROM VirtualPrivateGridStateEntry x WHERE x.stackId = :stackId AND x.userId = :userId AND x.timestamp <= :limit)", VirtualPrivateGridStateEntry.class);
      query.setParameter("limit", limit);
      query.setParameter("stackId", stackId);
      query.setParameter("userId", userId);
    
      results = query.getResultList();
      logger.debug("Getting active state for stack {}, user {}, upper limit date {} resultset {} ", stackId, userId, limit, results);
    } catch(NoResultException nre) {
        logger.warn("No active results found, stack {}, user {}, upper limit date {}", stackId, userId, limit);
      results = new ArrayList<VirtualPrivateGridStateEntry>();
    } catch (IllegalArgumentException ex) {
      if(logger.isDebugEnabled()) {
        logger.warn("Bad query for active state, stack {}, user {}, upper limit date {}", stackId, userId, limit);
      }
      results = new ArrayList<VirtualPrivateGridStateEntry>();
    }
    if (!results.isEmpty()) {
      return results.get(0);
    }
    if (logger.isDebugEnabled()) {
       logger.debug("No results getting active state for stack {}, user {}, upper limit date {}", stackId, userId, limit);
       List<VirtualPrivateGridStateEntry> all = get(stackId);
       for (VirtualPrivateGridStateEntry entry: all ) {
         logger.debug("getActiveState dump stack: stack {}, user {}, time {}, state {}", entry.getStackId(), entry.getUserId(), entry.getTimestamp(), entry.getState().name());
       }
    }
    return null;
  }
  
  
  @Override
  public List<String> getStackNames()
  {
    TypedQuery<String> query = emProvider.get().createQuery("select DISTINCT c.stackId from VirtualPrivateGridStateEntry c", String.class);
    List<String> results = null;
    try  {
      results = query.getResultList();
    } catch(NoResultException nre) {
      if(logger.isDebugEnabled()) {
        logger.debug(String.format("No distinct stack names found"));
      }
      results = new ArrayList<String>();
    }
    return results;
  }

  @Override
  public List<UUID> getUsers()
  {
    TypedQuery<UUID> query = emProvider.get().createQuery("select DISTINCT c.userId from VirtualPrivateGridStateEntry c", UUID.class);
    List<UUID> results = null;
    try {
      results = query.getResultList();
    } catch(NoResultException nre) {
      if(logger.isDebugEnabled()) {
        logger.debug("No distinct users found");
      }
      results = new ArrayList<UUID>();
    }
    return results;
  }

  @Override
  public List<UUID> getActivities()
  {
    TypedQuery<UUID> query = emProvider.get().createQuery("select DISTINCT c.activityId from VirtualPrivateGridStateEntry c", UUID.class);
    List<UUID> results = null;
    try {
      results = query.getResultList();
    } catch(NoResultException nre) {
      if(logger.isDebugEnabled()) {
        logger.debug("No distinct activities found");
      }
      results = new ArrayList<UUID>();
    }
    return results;
  }

  @Override
  /**
   * Get date of CREATE state event for the specified stack and user
   */
  public Date getCreationDate(String stackId, UUID userId) {
    List<VirtualPrivateGridStateEntry> results = null;
    try  {
      TypedQuery<VirtualPrivateGridStateEntry> query = emProvider.get().createQuery("from VirtualPrivateGridStateEntry c where c.stackId = :stackId and c.userId = :userId and c.state = :stackState", VirtualPrivateGridStateEntry.class);
      query.setParameter("stackId", stackId);
      query.setParameter("userId", userId);
      query.setParameter("stackState", VirtualPrivateGridStateEntry.State.CREATED);
      results = query.getResultList();
    } catch(NoResultException ex) {
      logger.warn("No results getting creation date for stack {} user {}", stackId, userId);
      results = null;
    } catch (NonUniqueResultException ex) {
      logger.warn("Multiple results getting creation date for stack {} user {}", stackId, userId);
      results = null;
    } catch (IllegalArgumentException ex) {
      logger.warn("Query error getting creation date for stack {} user {}", stackId, userId, ex);
      results = null;
    }

    if (null != results && !results.isEmpty()) {
      return results.get(0).getTimestamp();
    }
    
    if (logger.isDebugEnabled()) {
      logger.debug("No results getting creation date for stack {}, user {}", stackId, userId);
      List<VirtualPrivateGridStateEntry> all = get(stackId);
      for (VirtualPrivateGridStateEntry entry: all ) {
        logger.debug("getCreationDate dump stack: stack {}, user {}, time {}, state {}", entry.getStackId(), entry.getUserId(), entry.getTimestamp(), entry.getState().name());
      }
    }
    return null;
  }

  
  
  
  
}
