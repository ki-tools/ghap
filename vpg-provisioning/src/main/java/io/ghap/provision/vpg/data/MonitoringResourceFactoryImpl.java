package io.ghap.provision.vpg.data;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import io.ghap.provision.monitoring.lambda.CloudWatchMessage;
import io.ghap.provision.vpg.InstanceMonitoringState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.security.PublicKey;
import java.util.List;

/**
 */
public class MonitoringResourceFactoryImpl implements MonitoringResourceFactory {

    private Logger logger = LoggerFactory.getLogger(MonitoringResourceFactoryImpl.class);

    @Inject
    private Provider<EntityManager> emProvider;

    @Override
    @Transactional
    public InstanceMonitoringState create(CloudWatchMessage cloudWatchMessage, String stackId) {
      InstanceMonitoringState state = new InstanceMonitoringState();
      state.setAlarmDescription(cloudWatchMessage.AlarmDescription);
      state.setAlarmName(cloudWatchMessage.AlarmName);
      state.setInstanceId(cloudWatchMessage.Trigger.Dimensions.iterator().next().value);
      state.setMetric(cloudWatchMessage.Trigger.MetricName);
      state.setNewStateValue(cloudWatchMessage.NewStateValue);
      state.setOldStateValue(cloudWatchMessage.OldStateValue);
      state.setMessageDate(cloudWatchMessage.MessageTime.toDate());
      state.setMessageId(cloudWatchMessage.MessageId);
      state.setStackId(stackId);

      Gson gson = new Gson();
      state.setJsonMessage(gson.toJson(cloudWatchMessage));

      create(state);

      return state;
    }

    @Override
    @Transactional
    public void create(InstanceMonitoringState monitoringState) {

      emProvider.get().persist(monitoringState);

      Gson gson = new Gson();
      logger.debug("Added monitoring state for provisioned instance <" + gson.toJson(monitoringState));
    }

  @Override
    public List<InstanceMonitoringState> list() {
        return emProvider.get().createQuery("select e from InstanceMonitoringState e", InstanceMonitoringState.class).getResultList();
    }

    @Override
    public InstanceMonitoringState getByMessageId(String messageId) {

        TypedQuery<InstanceMonitoringState> query =
                emProvider.get().createQuery("from InstanceMonitoringState where MESSAGE_ID = :messageId",
                        InstanceMonitoringState.class);

        query.setParameter("messageId", messageId);

        InstanceMonitoringState foundMonitoredingState = null;
        try {
            foundMonitoredingState = query.getSingleResult();
        } catch(NoResultException nre) {
            if(logger.isDebugEnabled()) {
                logger.debug(String.format("No result found for message id %s", messageId));
            }
        }
        return foundMonitoredingState;

    }

    @Override
    public List<InstanceMonitoringState> getByStackId(String stackId) {

    TypedQuery<InstanceMonitoringState> query =
                emProvider.get().createQuery("from InstanceMonitoringState where STACK_ID = :stackId",
                        InstanceMonitoringState.class);

        query.setParameter("stackId", stackId);

        return query.getResultList();
    }

    @Override
    @Transactional
    public void delete(InstanceMonitoringState instanceMonitoringState) {
        emProvider.get().remove(instanceMonitoringState);
    }

    @Override
    @Transactional
    public void delete(List<InstanceMonitoringState> instanceMonitoringStates) {
        for (InstanceMonitoringState instanceMonitoringState : instanceMonitoringStates) {
            emProvider.get().remove(instanceMonitoringState);
        }
    }
}
