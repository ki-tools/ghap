package io.ghap.mailer;

import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STRawGroupDir;

import javax.annotation.PostConstruct;
import java.util.Map;

@Singleton
public class MailConfiguration {

    private STGroup stGroup;

    @Configuration("mail.config")
    private Map<String, ?> config;

    @PostConstruct
    public void loadMailTemplates(){
        this.stGroup = new STRawGroupDir("templates", '$', '$');
    }

    public ST getTemplate(String name){
        //see: http://stackoverflow.com/questions/14595500/how-to-escape-a-stringtemplate-template
        return stGroup.getInstanceOf(name);
    }

    public Map<String, ?> getSmtp(){
        return (Map<String, ?>)config.get("smtp");
    }

    public String getSmtpHost() {
        return String.valueOf(getSmtp().get("host"));
    }

    public int getSmtpPort() {
        try {
            return Integer.parseInt(String.valueOf(getSmtp().get("port")));
        } catch(NumberFormatException e){
            throw new NumberFormatException("LDAP port should be integer value");
        }
    }

    public boolean isSmtpAuth() {
        return String.valueOf(getSmtp().get("auth")).equalsIgnoreCase("true");
    }

    public boolean isSmtpStarttlsEnable() {
        return String.valueOf(getSmtp().get("starttls.enable")).equalsIgnoreCase("true");
    }

    public String getFrom() {
        return String.valueOf(config.get("from"));
    }

    public String getSmtpUser() {
        return String.valueOf(getSmtp().get("user"));
    }

    public String getSmtpPassword() {
        return String.valueOf(getSmtp().get("password"));
    }
}
