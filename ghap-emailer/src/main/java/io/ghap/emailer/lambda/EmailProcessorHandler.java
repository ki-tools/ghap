package io.ghap.emailer.lambda;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.gson.Gson;
import io.ghap.emailer.data.*;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.util.*;

/**
 * Lambda function that does a looks up by an email address from dynamodb, If the email has an entry in dynamo get
 * the alternate email address stored in dynamo db and resend the email to that atlernate address.  This is really just
 * a simply relay mechanisim to ingest emails sent to subdomain.ghap.io and send them on to a 'real' email address to
 * be consumed by a human.
 */
public class EmailProcessorHandler implements RequestStreamHandler {

  enum Verdict { PASS, FAIL, GRAY, PROCESSING_FAILED }
  private EmailFactory factory = new EmailFactoryImpl();
  private ConfigurationFactory configurationFactory = new ConfigurationFactoryImpl();

  private String host = configurationFactory.getConfiguration("lambda.emailer.host").getPropertyValue();
  private Configuration configuration = configurationFactory.getConfiguration("lambda.emailer.port");
  private int port = configuration != null ? Integer.parseInt(configuration.getPropertyValue()) : 25;
  private String smtp_username = configurationFactory.getConfiguration("lambda.emailer.smtp.username").getPropertyValue();
  private String smtp_password = configurationFactory.getConfiguration("lambda.emailer.smtp.password").getPropertyValue();
  private String bucket = configurationFactory.getConfiguration("lambda.emailer.mail.bucket").getPropertyValue();
  private String domain = configurationFactory.getConfiguration("lambda.emailer.mail.domain").getPropertyValue();

  @Override
  public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException
  {
    InputStreamReader streamReader = new InputStreamReader(input);
    Gson gson = new Gson();
    SESEvent event = gson.fromJson(streamReader, SESEvent.class);
    String result = handle(event, context);
    output.write(result.getBytes("utf-8"));
  }

  public String handle(SESEvent event, Context context) {
    context.getLogger().log("Inside the function\n");

    if(event == null || event.getRecords() == null) {
      context.getLogger().log("Event is null or there are no records\n");
      return "STOP_RULE";
    }

    SES ses = event.getRecords()[0].getSES();

    SESMail mail = ses.getMail();

    String messageId = mail.getMessageId();
    context.getLogger().log(String.format("MessageId = %s", messageId));

    try {
      if(isPotentialSpam(ses)) {
        context.getLogger().log("Dropping Spam\n");
        return "STOP_RULE";
      }

      List<Address> mappedRecipients = getMappedRecipients(ses, context);

      if(mappedRecipients.isEmpty()) {
        context.getLogger().log("Dropping Message no Mapped Recipients\n");
        return "STOP_RULE";
      }

      Session session = getMailSession(context);

      MimeMessage originalMimeMessage = getOriginalMimeMessage(session, messageId);

      Address[] from = originalMimeMessage.getFrom();
      originalMimeMessage.removeHeader("X-Auth-ID");
      originalMimeMessage.removeHeader("X-Sender-Id");
      originalMimeMessage.removeHeader("Return-Path");
      originalMimeMessage.removeHeader("Sender");
      originalMimeMessage.removeHeader("Source");
      originalMimeMessage.setRecipients(MimeMessage.RecipientType.TO, mappedRecipients.toArray(new Address[mappedRecipients.size()]));
      originalMimeMessage.setReplyTo(new Address[] {new InternetAddress(String.format("do-not-reply@%s",domain))});
      originalMimeMessage.setFrom(null);
      originalMimeMessage.setFrom(new InternetAddress(String.format("do-not-reply@%s", domain)));
      originalMimeMessage.addHeader("X-Auth-ID", String.format("do-not-reply@%s", domain));
      originalMimeMessage.addHeader("X-Sender-ID", String.format("do-not-reply@%s", domain));
      addOriginalSender(originalMimeMessage, context, from);
      originalMimeMessage.saveChanges();
      sendMessage(session, originalMimeMessage, context);

    } catch(Exception ae) {
      throw new RuntimeException(ae);
    } finally {
      deleteMessageFromS3(messageId);
    }
    return "CONTINUE";
  }

  private boolean isPotentialSpam(SES ses) {

    SESReceipt receipt = ses.getReceipt();
    return whatsTheVerdict(receipt.getDkimVerdict()) == Verdict.FAIL ||
            whatsTheVerdict(receipt.getSpamVerdict()) == Verdict.FAIL ||
            whatsTheVerdict(receipt.getSpfVerdict()) == Verdict.FAIL ||
            whatsTheVerdict(receipt.getVirusVerdict()) == Verdict.FAIL;
  }


  private List<Address> getMappedRecipients(SES ses, Context context) throws AddressException {
    List<String> recipients = ses.getReceipt().getRecipients();

    Set<String> mappedRecipients = new HashSet<>();
    for(String recipient: recipients) {
      context.getLogger().log(String.format("Looking for recipient mapping %s\n", recipient));
      Set<String> forwarders = factory.getMappedEmailAddresses(recipient);
      mappedRecipients.addAll(forwarders);
      context.getLogger().log(String.format("%d forwarders mapped.\n", forwarders.size()));
    }


    List<Address> addresses = new ArrayList<>();
    for(String mappedRecipient: mappedRecipients) {
      addresses.add(new InternetAddress(mappedRecipient));
    }

    return addresses;
  }

  private Verdict whatsTheVerdict(SESVerdict verdict) {
    return Verdict.valueOf(verdict.getStatus());
  }

  private Session getMailSession(Context context) {
    context.getLogger().log("Setting up properties");
    Properties props = new Properties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.port", port);
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.starttls.required", "true");

    context.getLogger().log("Getting Session");

    return Session.getDefaultInstance(props);
  }

  private MimeMessage getOriginalMimeMessage(Session session, String messageId) throws MessagingException {
    AmazonS3Client amazonS3Client = new AmazonS3Client(new MyAWSCredentialProvider());
    GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, messageId);
    S3Object messageObject = amazonS3Client.getObject(getObjectRequest);
    S3ObjectInputStream objectInputStream = messageObject.getObjectContent();
    MimeMessage message = new MimeMessage(session, objectInputStream);
    try {
      objectInputStream.close();
    } catch(IOException ioe) {
      // cleanup after ourselves.
    }
    return message;
  }

  private Multipart processMessageContent(MimeMessage originalMimeMessage, Context context) throws IOException, MessagingException {

    Object content = originalMimeMessage.getContent();

    if(content instanceof Multipart) {
      Multipart multipartContent = (Multipart)content;

      Multipart processedMultipartContent = new MimeMultipart("alternative");

      for(int position = 0; position < multipartContent.getCount(); position++) {
        BodyPart originalPart = multipartContent.getBodyPart(position);
        BodyPart part =new MimeBodyPart();;
        if(!originalPart.isMimeType("application/octet-stream")) {
          context.getLogger().log(String.format("Content Type for part %d is %s\n", position, originalPart.getContentType()));
          part.setContent(originalPart.getContent(), originalPart.getContentType());
        } else {
          context.getLogger().log("The attachment type is not supported and a body part indicating the attachment was stripped.");
          part.setContent(String.format("Attachment type of %s is not supported. %s has been removed.\n", originalPart.getContentType(), originalPart.getFileName()), "text/plain");
        }
        processedMultipartContent.addBodyPart(part);
      }

      return processedMultipartContent;

    } else {
      context.getLogger().log(String.format("%s unsupported content type", content.getClass()));
      return null;
    }

  }

  private void addOriginalSender(MimeMessage originalMimeMessage, Context context, Address[] addresses) throws IOException, MessagingException {
    if (addresses == null || addresses.length <= 0) {
      return;
    }
    context.getLogger().log("message content/type = " + originalMimeMessage.getContentType());
    Object content = originalMimeMessage.getContent();
    if (content == null) {
      context.getLogger().log("message content is null");
      return;
    }

    InternetAddress address = (InternetAddress) addresses[0];
    String signature = (address.getAddress() == null ? "" : address.getAddress()) + " " + (address.getPersonal() == null ? "" : address.getPersonal());
    if (content instanceof String) {
      if (originalMimeMessage.isMimeType("text/html")) {
        originalMimeMessage.setContent(content + "<br/><br/><p> This message from " + signature + "</p>", originalMimeMessage.getContentType());
      } else {
        originalMimeMessage.setContent(content + "\n This message from " + signature, originalMimeMessage.getContentType());
      }
    } else if (content instanceof Multipart) {
      Multipart multipartContent = (Multipart) content;
      ArrayList<BodyPart> parts = new ArrayList<>();
      getTextParts(multipartContent, parts);

      for (BodyPart originalPart : parts) {
        if (originalPart.isMimeType("text/html") || originalPart.isMimeType("text/plain")) {
          context.getLogger().log("message part = " + originalPart.getContent().toString());
          if (originalPart.isMimeType("text/html")) {
            originalPart.setContent(originalPart.getContent() + "<br/><br/><p> This message from " + signature + "</p>", originalPart.getContentType());
          } else {
            originalPart.setContent(originalPart.getContent() + "\n This message from " + signature, originalPart.getContentType());
          }
          context.getLogger().log("message part = " + originalPart.getContent().toString());
        }
      }
    }
  }

  private void getTextParts(Multipart mimeMultipart, List<BodyPart> parts) throws MessagingException, IOException {
    int count = mimeMultipart.getCount();
    for (int i = 0; i < count; i++) {
      BodyPart bodyPart = mimeMultipart.getBodyPart(i);
      if (bodyPart.isMimeType("text/plain") || bodyPart.isMimeType("text/html")) {
        parts.add(bodyPart);
      } else if (bodyPart.getContent() instanceof Multipart){
        getTextParts((Multipart) bodyPart.getContent(), parts);
      }
    }
  }

  private void sendMessage(Session session, MimeMessage message, Context context) throws MessagingException {
    Transport transport = null;

    try {
      transport = session.getTransport();
      context.getLogger().log("Connecting to SMTP Host\n");
      transport.connect(host, smtp_username, smtp_password);
      context.getLogger().log("Sending Message\n");
      transport.sendMessage(message, message.getAllRecipients());
      context.getLogger().log("Message Sent\n");

    } finally {
      if(transport != null) {
        try { transport.close(); } catch(MessagingException me) { context.getLogger().log(me.getMessage()); }
      }
    }

  }

  private void deleteMessageFromS3(String messageId) {
    AmazonS3Client amazonS3Client = new AmazonS3Client(new MyAWSCredentialProvider());
    DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucket, messageId);
    amazonS3Client.deleteObject(deleteObjectRequest);
  }

  private class MyAWSCredentialProvider implements AWSCredentialsProvider {
    private String s3_username = null;
    private String s3_password = null;

    MyAWSCredentialProvider() {
      refresh();
    }

    @Override
    public AWSCredentials getCredentials() {
      return new AWSCredentials() {
        @Override
        public String getAWSAccessKeyId() {
          return s3_username;
        }

        @Override
        public String getAWSSecretKey() {
          return s3_password;
        }
      };
    }

    @Override
    public void refresh() {
      s3_username = configurationFactory.getConfiguration("lambda.emailer.mail.s3.username").getPropertyValue();
      s3_password = configurationFactory.getConfiguration("lambda.emailer.mail.s3.password").getPropertyValue();
    }
  }
}
