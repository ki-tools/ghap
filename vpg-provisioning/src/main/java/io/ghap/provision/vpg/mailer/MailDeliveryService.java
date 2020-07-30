package io.ghap.provision.vpg.mailer;

import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class MailDeliveryService {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Configuration("enable.user.email.notifications")
    private String userEmailNotificationsEnabled;


    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void deliver(Email email) {
        executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                log.debug(String.format("Email notifications enabled ? %s", Boolean.valueOf(userEmailNotificationsEnabled)));

                if (Boolean.valueOf(userEmailNotificationsEnabled)) {
                    try {
                        email.send();
                    } catch (EmailException e) {
                        log.error("Cannot deliver email \"{}\".", email, e);
                    }

                }

                return null;
            }
        });
    }

}
