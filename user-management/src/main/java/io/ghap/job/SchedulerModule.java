package io.ghap.job;

import com.google.inject.AbstractModule;
import org.quartz.Scheduler;

/**
 *
 */
public class SchedulerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(SchedulerJobFactory.class);
        bind(Scheduler.class).toProvider(SchedulerProvider.class);
        bind(CronScheduler.class);
    }
}
