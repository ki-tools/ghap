package io.ghap.visualization.publish.data;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.google.gson.Gson;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.Responses;
import io.ghap.visualization.publish.data.validation.ManifestValidator;
import io.ghap.visualization.publish.data.validation.ValidationError;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.plexus.archiver.tar.GZipTarFile;
import org.codehaus.plexus.archiver.tar.TarEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import java.net.URL;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.DELETE_ON_CLOSE;
import static javax.ws.rs.core.Response.Status.*;


public class PublishDataFactoryImpl implements PublishDataFactory {

  private Logger log = LoggerFactory.getLogger(PublishDataFactoryImpl.class);

  @Configuration("visualizationpublication.s3.bucketname")
  private String bucketName;

  private AmazonS3Client s3;

  @PostConstruct
  public void init(){
    this.s3 = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
    if (!s3.doesBucketExist(bucketName)) {
      throw new IllegalStateException(String.format("Bucket %s does not exist.", bucketName));
    }
  }

  @Override
  public void prepare(String keyName) throws IllegalStateException {
    boolean exists = false;

    // bad practice to use an exception to control program
    // flow, but there isn't another way to check if the
    // key exists.
    try {
      ObjectMetadata data = s3.getObjectMetadata(bucketName, keyName);
      exists = true;
    } catch (AmazonServiceException ase) {
      if(ase.getStatusCode() != 404){
        throw ase;
      }
    }

    if (exists) {
      // if it exists in the s3 bucket already, delete it and replace
      // try 10 times
      long sleep_time = 1;
      for (int x = 0; x < 10; x++) {
        try {
          DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucketName, keyName);
          s3.deleteObject(deleteObjectRequest);
          break;
        } catch (AmazonServiceException ase) {
          if (ase.getStatusCode() == 400 && ase.getErrorCode().toLowerCase().equals("throttling")) {
            sleep_time *= 2;
          }
        }
      }
    }
  }

  @Override
  public AppPublishResult publish(File file, String type, String keyName, String meta) {

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
    metadata.setContentType(type);

    AppPublishResult publicationResult;

    // inspect the zipfile for the meta-data.json file and validate it
    GZipTarFile zipFile = new GZipTarFile(file);
    List<ValidationError> errors;
    ManifestValidator validator;

    try {
      validator = new ManifestValidator(zipFile, meta);
      errors = validator.validate();
    } catch (IOException e) {
      log.error("Cannot process application archive due to the following error", e);
      return new AppPublishResult(BAD_REQUEST, "Failed to process application archive");
    } finally {
      try {
        zipFile.close();
      } catch (IOException ignore) {}
    }

    if(errors == null || errors.isEmpty()){
      PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, keyName, file).withMetadata(metadata);
      PutObjectResult result = s3.putObject(putObjectRequest);

      File f = null;
      if(validator.getMetaData() == null){
        log.error("Cannot get meta-data, see above logs.");
        return new AppPublishResult(INTERNAL_SERVER_ERROR, "Cannot get meta-data. See logs.");
      }
      else try {
        f = File.createTempFile(file.getName() + "-", ".json");
        Files.write(f.toPath(), new Gson().toJson(validator.getMetaData()).getBytes(UTF_8), CREATE);
        String s3MetaDataFileName = keyName + ".json";
        log.debug("The following \"" + s3MetaDataFileName + "\" meta-data file was wrote: "  + validator.getMetaData());

        metadata = new ObjectMetadata();
        metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);

        putObjectRequest = new PutObjectRequest(bucketName, s3MetaDataFileName, f).withMetadata(metadata);
        result = s3.putObject(putObjectRequest);

      } catch (IOException e) {
        log.error("Cannot save temporary file with meta-data", e);
        return new AppPublishResult(INTERNAL_SERVER_ERROR, "Cannot save temporary file with meta-data");
      } finally {
        if(f != null){
          f.delete();
        }
      }

      publicationResult = new AppPublishResult(OK, null);
    }
    else {
      publicationResult = new AppPublishResult(PRECONDITION_FAILED, null);
      publicationResult.setErrors(errors);
    }

    return publicationResult;
  }


  /*
  public static void main(String[] args){
    GZipTarFile zipFile = new GZipTarFile(new File("F:/projects/ghap/temp/invalid-app-2.tar.gz"));
    List<ValidationError> errors = null;
    try {
      ManifestValidator validator = new ManifestValidator(zipFile);
      errors = validator.validate();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    finally {
      try {
        zipFile.close();
      } catch (IOException ignore) {}
    }

    AppPublishResult publicationResult = new AppPublishResult(PRECONDITION_FAILED, null);
    publicationResult.setErrors(errors);

    System.out.println(publicationResult);
  }
  */


  @Override
  public String registry(String url) {
    CloseableHttpClient client;
    CloseableHttpResponse response;
    StringBuffer content = new StringBuffer();
    try {
      client = getHttpClient();
      HttpGet get_registry = new HttpGet(url);
      response = client.execute(get_registry);
      if (response.getStatusLine().getStatusCode() == 200) {
        byte[] buffer = new byte[1024];
        InputStream contentStream = response.getEntity().getContent();
        for (int read = contentStream.read(buffer); read > 0; read = contentStream.read(buffer)) {
          content.append(new String(buffer, 0, read));
        }
      } else {
        content.append("{}");
      }
    } catch (IOException ioe) {
      log.error(ioe.getMessage(), ioe);
      content.append(ioe.getMessage());
    }
    return content.substring(0);
  }

  @Override
  public InputStream image(String url) {
    try {
      url = url.trim().toLowerCase();
      // ugly regexp to validate the url string
      if (!url.matches(
          "(^(https?:(\\\\\\\\|//))?[\\w\\-\\.]+(\\.ghap\\.io[\\\\\\/])[\\w\\-\\.\\\\\\/]+\\.(jpg|jpeg|png|gif|bmp|tif|tiff)$)")) {
        if (log.isErrorEnabled()) {
          log.error("Not a ghap.io image URL: %s\n", url);
        }
        return PublishDataFactoryImpl.class.getResourceAsStream("/Unknown.png");
      }
      CloseableHttpClient client = getHttpClient();
      HttpHead testUrl = new HttpHead(url);
      // checking that the url is accessible, without getting the body
      CloseableHttpResponse response = client.execute(testUrl);
      if (response.getStatusLine().getStatusCode() != 200) {
        if (log.isErrorEnabled()) {
          log.error(String.format("No access to URL: %s\n", url));
        }
        return PublishDataFactoryImpl.class.getResourceAsStream("/Unknown.png");
      }

      HttpGet get_registry = new HttpGet(url);
      response = client.execute(get_registry); // now get the full response body

      if (response.getStatusLine().getStatusCode() == 200) {
        InputStream contentStream = response.getEntity().getContent();
        // decoding an image from content stream
        BufferedImage image = ImageIO.read(contentStream);
        if (null != image) {
          ByteArrayOutputStream os = new ByteArrayOutputStream();
          String fileExtension = url.substring(url.lastIndexOf(".") + 1);
          ImageIO.write(image, fileExtension, os);
          ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
          os.close();
          return is; // returning an image stream
        }
      } else { // no image can be retrieved from that URL
        if (log.isErrorEnabled()) {
          log.error(String.format("No valid image accessible by URL: %s\n", url));
        }
        return PublishDataFactoryImpl.class.getResourceAsStream("/Unknown.png");
      }
    } catch (IOException ioe) {
      if (log.isErrorEnabled()) {
        log.error(String.format("IO Exception when getting image by URL: %s\n", url));
        log.error(ioe.getMessage(), ioe);
      }
    }
    return PublishDataFactoryImpl.class.getResourceAsStream("/Unknown.png");
  }

  private String getMessage(String field) {
    return String.format("%s is missing!\n", field);
  }

  private InputStream getStream(GZipTarFile zipFile, String name) {
    try {
      Enumeration entries = zipFile.getEntries();
      while (entries.hasMoreElements()) {
        TarEntry entry = (TarEntry) entries.nextElement();
        File path = new File(entry.getName());
        File fname = new File(name);
        String fileName = path.getName();
        if (fileName.equals(fname.getName())) {
          zipFile.getInputStream(entry);
          InputStream is = zipFile.getInputStream(entry);
          return is;
        }
      }
    } catch (IOException ioe) {
      if (log.isErrorEnabled()) {
        log.error(ioe.getMessage(), ioe);
      }
    }
    return null;
  }

  private CloseableHttpClient getHttpClient() {
    try {
      SSLContext sslContext = SSLContext.getInstance("SSL");

      // set up a TrustManager that trusts everything
      sslContext.init(null, new TrustManager[] { new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
      } }, new SecureRandom());

      return HttpClients.custom().setSslcontext(sslContext).setHostnameVerifier(new AllowAllHostnameVerifier()).build();
    } catch (Exception e) {
      return HttpClients.createDefault();
    }
  }
}
