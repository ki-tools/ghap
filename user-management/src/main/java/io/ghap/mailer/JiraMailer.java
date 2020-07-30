package io.ghap.mailer;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import io.ghap.user.model.User;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.mail.*;
import org.stringtemplate.v4.ST;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Singleton
public class JiraMailer extends AbstractMailer {
    private static final Map MAPPED_NAMES = ImmutableMap.builder()
            .put("ErrorSubmitter", asList("submitter", "user"))
            .put("sourceuser", "sourceuser")
            .put("CertaraProduct", "product")
            .put("Severity", "severity")
            .put("SeverityCategory", "severity-category")
            .put("Type", "type")
            .put("Abstract", "title")
            .put("Description", "description")
            .put("Customer", "customer")
            .put("Version", "version")
            .put("ProductEnvironment", "env")
            .put("ProductUser", "sourceuser")
            .put("Label", "label")
            .build();
    private static final List<String> EXCLUDE = asList("sourceuser", "ProductUser");

    @Configuration("jira.email") String jiraEmail;
    @Configuration("jira.email.from") String jiraEmailFrom;
    @Configuration("jira.email.subject") String jiraEmailSubject;

    public void send(FormDataMultiPart formParams) throws EmailException, IOException, MessagingException {
        MultiPartEmail email = getMail(jiraEmailFrom, "Certara", false);
        email.setCharset("iso-8859-1");
        email.addTo(jiraEmail);
        email.setSubject(jiraEmailSubject);

        Map<?, ?> data = prepareFormData(formParams);

        StringBuilder sb = new StringBuilder();
        sb.append("BMGF Error Report:\n");

        for(Map.Entry ent:data.entrySet()){
            if( !"description".equals(ent.getKey()) ) {
                sb.append(ent.getKey()).append("=").append(ent.getValue()).append("\n");
            }
        }

        sb.append("description").append("=\n").append(data.get("description")).append("\n");

        List<FormDataBodyPart> parts = formParams.getFields().values().stream().flatMap(List::stream).collect(Collectors.toList());

        if(parts != null) {
            for (FormDataBodyPart part : parts) {

                FormDataContentDisposition meta = part.getFormDataContentDisposition();

                if (meta.getFileName() != null) {
                    try (InputStream is = part.getEntityAs(InputStream.class)) {
                        // add the attachment
                        //And javax will automatically convert your file to base64
                        DataSource source = new ByteArrayDataSource(is, part.getMediaType().toString());
                        //DataSource source = new ByteArrayDataSource(new Base64InputStream(is, true), part.getMediaType().toString());
                        //email.attach(source, meta.getFileName(), "", EmailAttachment.INLINE);

                        // create a multipart leg for a specific attach
                        MimeMultipart mimeMultipart = new MimeMultipart();
                        BodyPart messageBodyPart = new MimeBodyPart();
                        messageBodyPart.setDataHandler(new DataHandler(source));

                        //messageBodyPart.removeHeader("Content-Transfer-Encoding");
                        messageBodyPart.addHeader("Content-Transfer-Encoding", "base64");

                        messageBodyPart.addHeader("name", meta.getFileName());

                        mimeMultipart.addBodyPart(messageBodyPart);
                        email.addPart(mimeMultipart);

                    }
                }
            }
        }

        email.setMsg(sb.toString());
        deliver(email);
    }

    private LinkedHashMap<String, String> prepareFormData(FormDataMultiPart formParams) {
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        Map<String, List<FormDataBodyPart>> fields = formParams.getFields();
        for (List<FormDataBodyPart> fieldValues : fields.values()) {
            for (FormDataBodyPart field : fieldValues) {
                if (field.isSimple()) {
                    if(EXCLUDE.contains(field.getName())){
                        // do not include to a result email
                        continue;
                    }

                    Object mappedNames = MAPPED_NAMES.get(field.getName());
                    if(mappedNames == null){
                        data.put(field.getName(), field.getValue());
                    }
                    else if(mappedNames instanceof Collection){
                        for(Object name:(Collection)mappedNames){
                            data.put(String.valueOf(name), field.getValue());
                        }
                    }
                    else {
                        data.put(mappedNames.toString(), field.getValue());
                    }
                }
            }
        }
        return data;
    }
}
