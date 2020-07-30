package io.ghap.mailer;


import com.google.inject.Injector;
import io.ghap.auth.LdapConfiguration;
import io.ghap.auth.LdapPrincipal;
import io.ghap.job.CronScheduler;
import io.ghap.ldap.LdapConnectionFactory;
import io.ghap.ldap.LdapUtils;
import io.ghap.ldap.template.LdapConnectionTemplate;
import io.ghap.user.dao.mapper.DomainEntryMapper;
import io.ghap.user.dao.mapper.UserEntryMapper;
import io.ghap.user.model.Domain;
import io.ghap.user.model.User;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Singleton
public class PasswordExpirationMailer extends AbstractMailer implements Job{

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject Injector injector;
    @Inject CronScheduler scheduler;
    @Inject LdapConfiguration ldapConfiguration;
    @Inject LdapConnectionFactory ldapConnectionFactory;

    @PostConstruct
    public void init() throws SchedulerException {
        // http://www.cronmaker.com/
        if(scheduler != null) {
            try {
                scheduler.submit("0 0 12 1/1 * ? *", this.getClass());
            }
            catch (ObjectAlreadyExistsException e){
                log.info(this.getClass().getSimpleName() + " was already scheduled");
            }
        }
        else {
            log.error("Cannot schedule \"" + this.getClass().getSimpleName() + "\" job. Scheduler is null.");
        }
        //scheduler.submitNow(this.getClass());
    }

    public String render(User user, long days){
        ST template = getTemplate("password-expiration");
        template.add("user", user);
        template.add("days", days);
        template.add("adminEmail", adminEmail);
        return render(template);
    }

    public void send(User user, long days) throws EmailException {
        Email email = getMail();
        if(user.getEmail() != null) {
            email.addTo(user.getEmail());

            String subject = t("mail.passwordExpiration.subject", "user", user);
            email.setSubject(subject);

            email.setMsg(render(user, days));
            deliver(email);
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        List<EmailInfo> emailList = getUsersToNotify();

        for(EmailInfo info:emailList){
            try {
                send(info.user, info.days);
            } catch (EmailException e) {
                log.error("Cannot notify user \"{}\"", info.user.getName(), e);
            }
        }
    }

    public List<EmailInfo> getUsersToNotify(){
        List<EmailInfo> emailList = new ArrayList<>();

        //go through all users and send email if need
        LdapPrincipal ldapPrincipal = ldapConfiguration.getAdmin();

        try(LdapConnection ldap = ldapConnectionFactory.get(ldapPrincipal) ){

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

            Domain domain = getDomain(ldap);

            // get all enabled users
            //see: https://support.microsoft.com/en-us/kb/269181
            String filter = "(&(objectClass=user)(!(userAccountControl:1.2.840.113556.1.4.803:=2)))";


            List<User> users = connectionTemplate.search(
                    connectionTemplate.newDn(domain.getDn()),
                    filter,
                    SearchScope.SUBTREE,
                    UserEntryMapper.getInstance());

            LocalDateTime now = LocalDateTime.now();
            for(User user:users){
                user.setDomainRelatedProperties( domain );

                Date dt = user.getPasswordExpiresDate();
                LocalDateTime ldt = LocalDateTime.ofInstant(dt.toInstant(), ZoneId.systemDefault());

                long days = now.until( ldt, ChronoUnit.DAYS);
                if(days > 0 && days <= 15){
                    emailList.add(new EmailInfo(user, days));
                }
            }

        } catch (LdapException e) {
            log.error("Cannot execute \"{}\" job", this.getClass().getSimpleName(), e);
        } catch (IOException e) {
            // hide exc exception
        }
        return emailList;
    }

    private Domain getDomain(LdapConnection ldap) throws LdapInvalidDnException {
        String realm = ldapConfiguration.getLdapRealm();
        Dn dn = LdapUtils.toDn(realm);

        LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);

        return connectionTemplate.lookup(dn, DomainEntryMapper.getInstance());
    }


    private static class EmailInfo {
        private final User user;
        private final long days;

        public EmailInfo(User user, long days){
            this.user = user;
            this.days = days;
        }
    }
}
