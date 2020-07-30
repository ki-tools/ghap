package io.ghap.mailer;

import io.ghap.user.manager.tracking.Change;
import io.ghap.user.model.User;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.stringtemplate.v4.ST;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.MissingResourceException;

@Singleton
public class UpdateUserMailer extends AbstractMailer {

    public String render(User user, Collection<Change> changes, boolean hidePassword){
        ST template = getTemplate("update-user");
        Collection<Change> translatedChanges = new ArrayList<>(changes.size());
        for(Change c:changes){
            String title = c.getField();
            String key = "user.fields." + c.getField() + ".title";
            try {
                title = t(key);
            } catch(MissingResourceException e){
                log.error("Cannot retrieve \"" + key + "\" field translation for \"" + template.getName() + "\" email template");
            }
            if(title != null && !title.trim().isEmpty()) {
                translatedChanges.add(new Change(title, c.getOldValue(), c.getNewValue(), hidePassword && "password".equals(c.getField())));
            }
        }
        template.add("user", user);
        template.add("changes", translatedChanges);
        template.add("adminEmail", adminEmail);
        return render(template);
    }

    public void send(User user, List<Change> changes, boolean hidePassword) throws EmailException {
        Email email = getMail();
        if(user.getEmail() != null) {
            email.addTo(user.getEmail());

            String subject = t("mail.updateUser.subject", "user", user );
            email.setSubject(subject);

            email.setMsg(render(user, changes, hidePassword));
            deliver(email);
        }
    }
}

