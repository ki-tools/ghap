package io.ghap.jetty;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.GuiceFilter;
import com.netflix.governator.DefaultLifecycleListener;
import com.netflix.governator.LifecycleListener;
import com.netflix.governator.LifecycleManager;
import com.netflix.governator.LifecycleShutdownSignal;
import com.netflix.governator.guice.jetty.DefaultJettyConfig;
import com.netflix.governator.guice.jetty.JettyConfig;
import com.netflix.governator.guice.jetty.JettyLifecycleShutdownSignal;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.DispatcherType;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.EnumSet;

public class TestJettyModule extends AbstractModule {
  private final String username = "tester";
  private final String password = "testing";

  private final static Logger LOG = LoggerFactory.getLogger(TestJettyModule.class);

  private int port = Integer.parseInt(System.getProperty("initial.server.port","10800"));

  private static ServerSocket serverSocket;

  /**
   * Eager singleton to start the Jetty Server
   *
   * @author elandau
   */
  @Singleton
  public static class JettyRunner {
    @Inject
    public JettyRunner(Server server, final LifecycleManager manager) {
      LOG.info("Jetty server starting");
      try {
        serverSocket.close();
        server.start();
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        LOG.info("Jetty server on port {} started", port);
      } catch (Exception e) {
        try {
          server.stop();
        } catch (Exception e2) {
        }
        throw new ProvisionException("Jetty server failed to start", e);
      }
    }
  }

  /**
   * LifecycleListener to stop Jetty Server.  This will catch shutting down
   * Jetty when notified only through LifecycleManager#shutdown() and not via the
   * LifecycleEvent#shutdown().
   *
   * @author elandau
   */
  @Singleton
  public static class JettyShutdown extends DefaultLifecycleListener {
    private Server server;

    @Inject
    public JettyShutdown(Server server) {
      this.server = server;
    }

    @Override
    public void onStopped() {
      LOG.info("Jetty Server shutting down");
      try {
        Thread t = new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              server.stop();
              LOG.info("Jetty Server shut down");
            } catch (Exception e) {
              LOG.warn("Failed to shut down Jetty server", e);
            }
          }
        });
        t.start();
      } catch (Exception e) {
        LOG.warn("Error shutting down Jetty server");
      }
    }
  }

  @Override
  protected void configure() {
    bind(JettyRunner.class).asEagerSingleton();
    Multibinder.newSetBinder(binder(), LifecycleListener.class).addBinding().to(JettyShutdown.class);
    bind(LifecycleShutdownSignal.class).to(JettyLifecycleShutdownSignal.class);
  }

  @Provides
  @Singleton
  private JettyConfig getDefaultConfig() {
    for(int x = port; ; x++) {
      try {
        serverSocket = new ServerSocket(x);
        port = x;
        break;
      } catch(IOException ioe) {
        if(LOG.isErrorEnabled()) {
          LOG.error(ioe.getMessage(), ioe);
        }
      }
      try {
        if(serverSocket != null) {
          serverSocket.close();
        }
      } catch(IOException ioe) {
        if(LOG.isErrorEnabled()) {
          LOG.error(ioe.getMessage(), ioe);
        }
      }
    }

    DefaultJettyConfig config = new DefaultJettyConfig();
    config.setPort(port);
    return config;
  }

  @Provides
  @Singleton
  private Server getServer(JettyConfig config) {
    ConstraintSecurityHandler security = new ConstraintSecurityHandler();

    Constraint constraint = new Constraint();
    constraint.setName(Constraint.__BASIC_AUTH);
    constraint.setAuthenticate(true);
    constraint.setRoles(new String[]{"user", "admin"});

    ConstraintMapping mapping = new ConstraintMapping();
    mapping.setPathSpec("/*");
    mapping.setConstraint(constraint);

    HashLoginService loginService = new HashLoginService("Test");
    String[] roles = {"user", "admin"};
    loginService.putUser(username, Credential.getCredential(password), roles);

    security.setConstraintMappings(Collections.singletonList(mapping));
    security.setLoginService(loginService);
    security.setAuthenticator(new BasicAuthenticator());

    Server server = new Server(config.getPort());
    server.setHandler(security);
    server.addBean(loginService);

    ServletContextHandler servletContextHandler = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
    servletContextHandler.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
    servletContextHandler.addServlet(DefaultServlet.class, "/");
    servletContextHandler.setSecurityHandler(security);
    return server;
  }
}
