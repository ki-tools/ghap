package io.ghap.security;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Servlet implementation class ConfigurationServlet
 */
public class RemotePrivateIpServlet extends HttpServlet {

  public final static String X_FORWARDED_FOR = "X-Forwarded-For";
  public final static String REMOTE_ADDR = "REMOTE_ADDR";

  public RemotePrivateIpServlet() {
    super();
  }

  public void init(ServletConfig config) throws ServletException {}

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String forwarded_for = req.getHeader(X_FORWARDED_FOR);
    String client = req.getRemoteAddr();
    if (forwarded_for != null && !forwarded_for.isEmpty()) {
      StringTokenizer toker = new StringTokenizer(forwarded_for, ",", false);
     if (toker.hasMoreTokens()) {
        client = toker.nextToken();
      }
    }

    AWSCredentialsProvider credentials = new SystemPropertiesCredentialsProvider();
    AmazonEC2Client ec2Client = new AmazonEC2Client(credentials);

    DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
    DescribeInstancesResult results = ec2Client.describeInstances(describeInstancesRequest);

    for(String token = ""; token != null; token = results.getNextToken()) {
      boolean shouldbreak = false;
      for(Reservation reservation : results.getReservations()) {
        for(Instance instance: reservation.getInstances()) {
          String ip = instance.getPublicIpAddress();
          if(ip != null && ip.equals(client)) {
            client = instance.getPrivateIpAddress();
            shouldbreak = true;
            break;
          }
        }
        if(shouldbreak) {
          break;
        }
      }
      if(shouldbreak) {
        break;
      }
      describeInstancesRequest = new DescribeInstancesRequest();
      describeInstancesRequest.setNextToken(token);
      results = ec2Client.describeInstances(describeInstancesRequest);
    }


    byte[] bytes = client.getBytes();
    resp.setContentType("text/plain");
    resp.setContentLength(bytes.length);
    resp.getOutputStream().write(bytes);
  }
}
