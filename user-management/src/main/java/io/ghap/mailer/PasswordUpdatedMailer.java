package io.ghap.mailer;


import io.ghap.user.model.User;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.stringtemplate.v4.ST;

import javax.inject.Singleton;

@Singleton
public class PasswordUpdatedMailer extends AbstractMailer {

    public String render(User user, boolean forgotPassword){
        ST template = forgotPassword ? getTemplate("password-updated-by-forgot-password") : getTemplate("password-updated");
        template.add("user", user);
        template.add("adminEmail", adminEmail);
        return render(template);
    }

    public void send(User user, boolean forgotPassword) throws EmailException {
        Email email = getMail();
        if(user.getEmail() != null) {
            email.addTo(user.getEmail());

            String subject = t("mail.passwordUpdated.subject", "user", user );
            email.setSubject(subject);

            email.setMsg(render(user, forgotPassword));
            deliver(email);
        }
    }

}
