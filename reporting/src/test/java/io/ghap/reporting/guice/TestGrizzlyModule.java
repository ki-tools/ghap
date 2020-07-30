package io.ghap.reporting.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.GuiceFilter;
import com.netflix.governator.DefaultLifecycleListener;
import com.netflix.governator.LifecycleListener;
import com.netflix.governator.guice.jetty.DefaultJettyConfig;
import com.netflix.governator.guice.jetty.JettyConfig;
import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.http.servlet.ServletAdapter;
import io.ghap.reporting.bootstrap.Application;
import io.ghap.reporting.bootstrap.ReportingBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

public class TestGrizzlyModule extends AbstractModule {
  private final static Logger LOG = LoggerFactory.getLogger(TestGrizzlyModule.class);

  private int port = Integer.parseInt(System.getProperty("initial.server.port", "10800"));

  /**
   * Eager singleton to start the Jetty Server
   *
   * @author elandau
   */
  @Singleton
  public static class GrizzlyRunner {
    @Inject
    public GrizzlyRunner(GrizzlyWebServer server) {
      LOG.info("Jetty server starting");
      try {
        server.start();
      } catch (IOException e) {
        LOG.error("error start server", e);
        server.stop();
        throw new RuntimeException(e);
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
  public static class GrizzlyShutdown extends DefaultLifecycleListener {
    private GrizzlyWebServer server;

    @Inject
    public GrizzlyShutdown(GrizzlyWebServer server) {
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
    bind(GrizzlyRunner.class).asEagerSingleton();
    Multibinder.newSetBinder(binder(), LifecycleListener.class).addBinding().to(GrizzlyShutdown.class);
  }

  @Provides
  @Singleton
  private JettyConfig getDefaultConfig() {
    DefaultJettyConfig config = new DefaultJettyConfig();
    config.setPort(port);
    return config;
  }

  @Provides
  @Singleton
  private GrizzlyWebServer getServer(JettyConfig config) {
    GrizzlyWebServer server = new GrizzlyWebServer(config.getPort());
    ServletAdapter adapter = new ServletAdapter(new Application.DummySevlet());

    adapter.addContextParameter("governator.bootstrap.class", ReportingBootstrap.class.getName());

    adapter.addServletListener(com.netflix.governator.guice.servlet.GovernatorServletContextListener.class.getName());
    adapter.addFilter(new GuiceFilter(), "guiceFilter", null);
    server.addGrizzlyAdapter(adapter, new String[]{"/"});
    return server;
  }
}
