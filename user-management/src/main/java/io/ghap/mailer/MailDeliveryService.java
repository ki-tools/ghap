package io.ghap.mailer;

import com.google.inject.Singleton;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class MailDeliveryService {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void deliver(Email email){
        executor.submit(() -> {
            try {
                email.send();
            } catch (EmailException e) {
                log.error("Cannot deliver email \"{}\".",email, e);
            }
        });
    }


}
