package io.ghap.user.manager.impl;

import com.sun.jersey.multipart.FormDataMultiPart;
import io.ghap.mailer.JiraMailer;
import io.ghap.mailer.UpdateUserMailer;
import io.ghap.user.manager.JiraService;
import org.apache.commons.mail.EmailException;

import javax.inject.Inject;
import javax.mail.MessagingException;
import java.io.IOException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("issues")
public class DefaultJiraService implements JiraService {
    @Inject
    private JiraMailer jiraMailer;

    @Override
    @POST
    @Path("/submit-error")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public String bugReport(FormDataMultiPart formParams) {
        try {
            jiraMailer.send(formParams);
        } catch (Exception e) {
            return "Failed";
        }
        return "Success";
    }
}
