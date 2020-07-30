package io.ghap.job;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.config.ConfigurationManager;
import org.quartz.*;

import java.util.Date;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Singleton
public class CronScheduler {

    @Inject
    Scheduler scheduler;

    public Date submit(String cronExpression, Class jobClass) throws SchedulerException {
        String environment = ConfigurationManager.getDeploymentContext().getDeploymentEnvironment();

        if( !"test".equalsIgnoreCase(environment) ) {
            String name = jobClass.getSimpleName();
            JobDetail job = newJob(jobClass).withIdentity(name, "cron").build();

            CronTrigger trigger = newTrigger().withIdentity(name, "cron").withSchedule(cronSchedule(cronExpression))
                    .build();

            return scheduler.scheduleJob(job, trigger);
        } else {
            return null;
        }
    }

    public Date submitNow(Class jobClass) throws SchedulerException {
        String name = jobClass.getSimpleName();
        JobDetail job = newJob(jobClass).withIdentity(name, "immediately").build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(name, "immediately")
                .startNow()
                .build();

        return scheduler.scheduleJob(job, trigger);
    }

}
