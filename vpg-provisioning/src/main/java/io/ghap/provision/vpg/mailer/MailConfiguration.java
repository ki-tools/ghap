package io.ghap.provision.vpg.mailer;

import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;

import java.util.Map;

@Singleton
public class MailConfiguration {

    @Configuration("mail.config")
    private Map<String, ?> config;

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
            throw new NumberFormatException("SMTP port should be integer value");
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
