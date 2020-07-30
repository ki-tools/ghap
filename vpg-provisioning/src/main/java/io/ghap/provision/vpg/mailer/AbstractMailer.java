package io.ghap.provision.vpg.mailer;

import com.netflix.governator.annotations.Configuration;
import io.ghap.util.ContentTemplateAccessor;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.ResourceBundle;

public abstract class AbstractMailer {

  protected final Logger log = LoggerFactory.getLogger(this.getClass());

  @Inject
  MailDeliveryService mailDeliveryService;


  @Inject
  MailConfiguration config;

  @Configuration("admin.email")
  String adminEmail;

  @Inject
  private ContentTemplateAccessor contentTemplateAccessor;

  @Inject
  @Named("defaultMessages")
  private ResourceBundle messages;

  protected Email getMail(String from) throws EmailException {
    HtmlEmail email = new HtmlEmail();
    email.setDebug(true);

    email.setHostName(config.getSmtpHost());
    email.setSmtpPort(config.getSmtpPort());

    if (config.isSmtpAuth())
      email.setAuthentication(config.getSmtpUser(), config.getSmtpPassword());

    if (config.isSmtpStarttlsEnable()) {
      email.setStartTLSEnabled(true);
      //email.setSSLOnConnect(true);
      email.setSslSmtpPort(Integer.toString(config.getSmtpPort()));
    }


    email.setFrom(from != null ? from : config.getFrom());
    return email;
  }

  protected void deliver(Email email) {
    mailDeliveryService.deliver(email);
  }

  protected String getTemplate(String name, Map<String, String> parameters) {
    return contentTemplateAccessor.getTemplate(name, parameters);
  }

  protected String t(String key, Object... args) {
    return format(messages.getString(key), args);
  }

  protected String format(String template, Object... args) {
    ST st = new ST(template, '$', '$');

    for (int i = 0; i < args.length; i = i + 2) {
      st.add(args[i].toString(), args[i + 1]);
    }
    return st.render();
  }

  protected Email getMail() throws EmailException {
    return getMail(null);
  }


}
