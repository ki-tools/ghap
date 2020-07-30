package io.ghap.provision.vpg.mailer;


import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;

import javax.inject.Singleton;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class IdleResourceNotificationMailer extends AbstractMailer {

  public String render(String userName, String activityName, String activityOsType,
                       String activityIPAddress, String urlToPostponeScheduledStop, Date scheduledStopTime) {

    Map<String, String> parameters = new HashMap<>();
    parameters.put("userName", userName);
    parameters.put("adminEmail", adminEmail);
    parameters.put("urlToPostponeScheduledStop", urlToPostponeScheduledStop);

    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy 'at' h:mm aaa z");
    parameters.put("scheduledStopTime", dateFormat.format(scheduledStopTime));

    parameters.put("activityName", activityName);
    parameters.put("activityOsType", activityOsType);
    parameters.put("activityIPAddress", activityIPAddress);

    return getTemplate("notify-user-idle-resources", parameters);
  }

  public void send(String userName, String userEmail, String activityName,
                   String activityOsType, String activityIPAddress,
                   String urlToPostponeScheduledStop, Date scheduledStopTime)
          throws EmailException {

    Email email = getMail();
    if (userEmail != null) {
      email.addTo(userEmail);

      String subject = t("mail.idle.resource.notification.subject", "userName", userName);
      email.setSubject(subject);

      email.setMsg(render(userName, activityName, activityOsType, activityIPAddress,
              urlToPostponeScheduledStop, scheduledStopTime));

      deliver(email);
    }
  }
}
