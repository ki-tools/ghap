package io.ghap.mailer;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import com.netflix.governator.annotations.Configuration;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.internet.AddressException;
import java.util.ResourceBundle;

public abstract class AbstractMailer {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject MailDeliveryService mailDeliveryService;

    @Inject MailConfiguration config;

    @Configuration("admin.email") String adminEmail;

    @Inject
    @Named("defaultMessages")
    private ResourceBundle messages;

    protected HtmlEmail getMail(String from) throws EmailException {
        return getMail(from, null);
    }

    protected HtmlEmail getMail(String from, String fromName) throws EmailException {
        return (HtmlEmail)getMail(from, fromName, true);
    }

    protected MultiPartEmail getMail(String from, String fromName, boolean isHtml) throws EmailException {
        MultiPartEmail email = isHtml ? new HtmlEmail(): new MultiPartEmail();
        email.setDebug(true);

        email.setHostName(config.getSmtpHost());
        email.setSmtpPort(config.getSmtpPort());

        if( config.isSmtpAuth() )
            email.setAuthentication(config.getSmtpUser(), config.getSmtpPassword());

        if( config.isSmtpStarttlsEnable() ) {
            email.setStartTLSEnabled(true);
            //email.setSSLOnConnect(true);
            email.setSslSmtpPort( Integer.toString( config.getSmtpPort() ) );
        }


        email.setFrom(from != null ? from : config.getFrom(), fromName);
        return email;
    }

    protected void deliver(Email email){
        mailDeliveryService.deliver(email);
    }

    protected ST getTemplate(String name){
        return config.getTemplate(name);
    }

    protected String t(String key, Object... args){
        return format(messages.getString(key), args);
    }

    protected String format(String template, Object... args){
        ST st = new ST(template, '$','$');

        for(int i=0; i< args.length;i=i+2){
            st.add(args[i].toString(), args[i+1]);
        }
        return st.render();
    }

    protected HtmlEmail getMail() throws EmailException {
        return getMail(null);
    }

    protected String render(ST template){
        //String compressedHtml = template.render().replaceAll(">\\s+<", "><");
        HtmlCompressor compressor = new HtmlCompressor();
        compressor.setRemoveIntertagSpaces(true);
        String compressedHtml = compressor.compress(template.render());
        return compressedHtml;
    }

}
