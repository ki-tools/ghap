package io.ghap.mailer;

import io.ghap.user.model.User;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.stringtemplate.v4.ST;

import javax.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URL;

@Singleton
public class PasswordResetMailer extends AbstractMailer {
    public String render(User user, String urlPattern, String token){
        ST template = getTemplate("password-reset");
        template.add("user", user);
        template.add("url", format(urlPattern, "token", token));
        template.add("baseUrl", getBaseUrl(urlPattern));
        template.add("adminEmail", adminEmail);
        return render(template);
    }

    private static String getBaseUrl(String urlString)
    {

        if(urlString == null)
        {
            return null;
        }

        try
        {
            URL url = new URL(urlString);
            return url.getProtocol() + "://" + url.getAuthority();
        }
        catch (MalformedURLException e)
        {
            try {
                String prefix = urlString.substring(0, 8);
                String suffix = urlString.substring(8);
                int pos = suffix.indexOf('/');
                return prefix + suffix.substring(0, pos);
            }
            catch (Exception ex){
                throw new RuntimeException(new MalformedURLException("Cannot parse URL \"" + urlString + "\""));
            }
        }
    }

    public void send(User user, String urlPattern, String token) throws EmailException {
        Email email = getMail();
        if(user.getEmail() != null) {
            email.addTo(user.getEmail());

            String subject = t("mail.passwordReset.subject", "user", user );
            email.setSubject(subject);

            email.setMsg(render(user, urlPattern, token));
            deliver(email);
        }
    }
}
