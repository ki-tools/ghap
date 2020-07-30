package io.ghap.activity;

import com.google.inject.servlet.GuiceFilter;
import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.http.servlet.ServletAdapter;

import javax.servlet.http.HttpServlet;


public class Application {

    @SuppressWarnings("serial")
    public static class DummySevlet extends HttpServlet { }

    public static void main(String[] args) throws Exception {
        int port = Integer.valueOf(System.getProperty("port", "8080"));
        GrizzlyWebServer server = new GrizzlyWebServer(port);
        ServletAdapter adapter = new ServletAdapter(new DummySevlet());

        adapter.addContextParameter("governator.bootstrap.class", ActivityManagerBootstrap.class.getName());

        adapter.addServletListener(com.netflix.governator.guice.servlet.GovernatorServletContextListener.class.getName());
        adapter.addFilter(new GuiceFilter(), "guiceFilter", null);
        server.addGrizzlyAdapter(adapter, new String[]{ "/" });
        server.start();
    }
}
