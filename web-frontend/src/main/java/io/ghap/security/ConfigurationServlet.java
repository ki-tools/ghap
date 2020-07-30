package io.ghap.security;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class ConfigurationServlet extends HttpServlet {
    private static final long serialVersionUID = 2853208973402346562L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ConfigurationServlet() {
        super();
    }

    public void init(ServletConfig config) throws ServletException {
        Configuration.setConfiguration(new Configuration() {

            @Override
            @SuppressWarnings("unchecked")
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                @SuppressWarnings("rawtypes")
                Map options = new HashMap();
                AppConfigurationEntry adModule = new AppConfigurationEntry(ADLoginModule.class.getName(), LoginModuleControlFlag.REQUIRED, options);
                return new AppConfigurationEntry[]{adModule};
            }
        });



        String env = System.getProperty("archaius.deployment.environment");
        if( env == null) {
            System.setProperty("archaius.deployment.environment", env = "dev");
        }

        ConfigurationManager.getDeploymentContext().setDeploymentEnvironment(env);


        // For some reason(bug) cascaded properties doesn't work with "config" file name...
        try {
            ConfigurationManager.loadCascadedPropertiesFromResources("application");
        } catch (IOException e) {
            throw new ServletException(e);
        }

        S3Configuration.init(
                "config/web-frontend-" + env + ".properties",
                "config/application-" + env + ".properties"
        );

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        DynamicProperty prop =  DynamicProperty.getInstance("config");
        String config =  (prop == null) ? null:prop.getString();

        if(config == null){
            config="{}";
        }

        byte[] bytes = config.getBytes(StandardCharsets.UTF_8);
        resp.setContentLength(bytes.length);
        resp.setContentType("application/json");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getOutputStream().write(bytes);
    }
}
