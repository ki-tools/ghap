package io.ghap.user.manager.impl;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.NotFoundException;
import io.ghap.mailer.NewUserMailer;
import io.ghap.mailer.PasswordExpirationMailer;
import io.ghap.mailer.UpdateUserMailer;
import io.ghap.user.dao.DomainDao;
import io.ghap.user.dao.UserDao;
import io.ghap.user.manager.PreviewEmailService;
import io.ghap.user.manager.tracking.Change;
import io.ghap.user.model.Domain;
import io.ghap.user.model.User;
import org.apache.directory.api.ldap.model.exception.LdapException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

@Path("email/preview")
@Singleton
public class DefaultPreviewEmailService implements PreviewEmailService {

    @Inject UserDao userDao;
    @Inject DomainDao domainDao;

    @Inject NewUserMailer newUserMailer;
    @Inject UpdateUserMailer updateUserMailer;
    @Inject PasswordExpirationMailer passwordExpirationMailer;

    @Override
    @GET
    @Path("new-user/{dn}")
    @Produces(TEXT_HTML)
    public String newUserEmailPreview(@PathParam("dn") String dn) throws LdapException {
        User user = getUser(dn);
        user.setPassword("********");
        return newUserMailer.render(user);
    }

    @Override
    @GET
    @Path("update-user/{dn}")
    @Produces(TEXT_HTML)
    public String updateUserEmailPreview(@PathParam("dn") String dn) throws LdapException {
        User user = getUser(dn);
        Collection<Change> changes = ImmutableList.of(
                new Change("firstName", "Bob", "Jack"),
                new Change("lastName", "Black", "Brown"),
                new Change("password", null, "")
        );

        return updateUserMailer.render(user,changes, false);
    }

    @Override
    @GET
    @Path("password-expiration/{dn}")
    @Produces(TEXT_HTML)
    public String passwordExpiraionEmailPreview(@PathParam("dn") String dn) throws LdapException {
        User user = getUser(dn);
        user.setDomainRelatedProperties(domainDao.find());

        LocalDateTime now = LocalDateTime.now();
        Date dt = user.getPasswordExpiresDate();
        LocalDateTime ldt = LocalDateTime.ofInstant(dt.toInstant(), ZoneId.systemDefault());

        long days = now.until(ldt, ChronoUnit.DAYS);

        List users = passwordExpirationMailer.getUsersToNotify();
        System.out.println(users);

        return passwordExpirationMailer.render(user, days);
    }

    private User getUser(String dn) throws LdapException {
        User user = userDao.find(dn);
        if(user == null) {
            throw new NotFoundException("Cannot find user \""+dn+"\"");
        }
        else {
            user.setDomainRelatedProperties( domainDao.find() );
        }
        return user;
    }
}
