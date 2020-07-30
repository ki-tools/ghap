package io.ghap.security;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class ConfigurationServlet
 */
public class RemoteIpServlet extends HttpServlet {

  public final static String X_FORWARDED_FOR = "X-Forwarded-For";
  public final static String REMOTE_ADDR = "REMOTE_ADDR";

  public RemoteIpServlet() {
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
    byte[] bytes = client.getBytes();
    resp.setContentType("text/plain");
    resp.setContentLength(bytes.length);
    resp.getOutputStream().write(bytes);
  }
}
