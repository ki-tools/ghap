package io.ghap.web;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicContextualProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.*;

/**
 *
 */
public class Bootstrap {

    private static final Logger LOG = Logger.getLogger( Bootstrap.class );

    public static void configure(ServletContext context) {

        /** Store the file path to the basic URL.  When we're not running as
         a servlet, it defaults to the user's current directory. */
        String rootPath = context.getRealPath("/");

        try {

            /*
            System.setProperty( "extra.controls",
                    "org.apache.directory.api.ldap.extras.controls.policy_impl.PasswordPolicyFactory"
            );
            */


            String env = System.getProperty("archaius.deployment.environment");
            if( env == null) {
                System.setProperty("archaius.deployment.environment", env = "dev");
            }

            // configure logger
            if( System.getProperty("catalina.base") == null ){
                System.setProperty("catalina.base", new File("").getAbsolutePath());
            }

            org.apache.log4j.LogManager.resetConfiguration( );
            if ( System.getProperty( "log4j.config" ) != null ) {
                PropertyConfigurator.configureAndWatch(System.getProperty("log4j.config"));
            }
            else {
                PropertyConfigurator.configure(Loader.getResource("log4j-" + env + ".properties"));
            }

            ConfigurationManager.getDeploymentContext().setDeploymentEnvironment(env);

            // For some reason(bug) cascaded properties doesn't work with "config" file name...
            ConfigurationManager.loadCascadedPropertiesFromResources("application");
            ConfigurationManager.loadCascadedPropertiesFromResources("ghap-knowledgebase");
            S3Configuration.init(
                    "config/ghap-knowledgebase-" + env + ".properties",
                    "config/application-" + env + ".properties");

            Properties jspWikiConfig = new Properties();

            setPolicyProperties(jspWikiConfig, rootPath, env);

            Map<String, ?> mail = getMailConfig();
            if( !mail.isEmpty() ){
                PropertyTranslation etl = new PropertyTranslation(jspWikiConfig, mail);
                etl.translate("mail.smtp.host", "smtp", "host");
                etl.translate("mail.smtp.port", "smtp", "port");
                etl.translate("mail.from", "from");
                etl.translate("mail.smtp.account",  "smtp", "user");
                etl.translate("mail.smtp.password", "smtp", "password");
                etl.translate("mail.smtp.starttls.enable", "smtp", "starttls.enable");
            }

            saveProperties(jspWikiConfig, rootPath, env);

            //disable ehcache to prevent page cache in a memory
            boolean usePageCache = "true".equals(ConfigurationManager.getConfigInstance().getString("jspwiki.usePageCache"));
            System.setProperty("net.sf.ehcache.disabled", String.valueOf(!usePageCache));


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static Map<String, ?> getMailConfig(){
        String mailConfig = DynamicPropertyFactory.getInstance().getStringProperty("mail.config", "{}").getValue();
        Map<String, Object> config = Collections.emptyMap();
        try {
            config = new ObjectMapper().readValue(mailConfig, HashMap.class);
        } catch (IOException e) {
            LOG.error("Cannot read \"mail.config\" property: " + e);
        }
        return config;
    }

    private static void setPolicyProperties(Properties jspWikiConfig, String rootPath, String env){
        // init policy file "jspwiki.policy.file"
        File policyConfigFile = new File(rootPath,"/WEB-INF/jspwiki-custom.policy");
        String s3PolicyConfigFile = "config/ghap-knowledgebase-" + env + ".policy";
        S3Client s3 = S3Client.getInstance();

        boolean usePolicyFromS3 = false;
        try(InputStream is = s3.getConfigFile(s3PolicyConfigFile); OutputStream os = new FileOutputStream(policyConfigFile)){
            IOUtils.copy(is, os);
            usePolicyFromS3 = true;
        }
        catch (Exception e){
            LOG.error("Cannot load policy from S3 file \"" + s3PolicyConfigFile + "\"" +
                    " (bucket: " + S3Client.getConfigurationBucket() + ") due to the following error: " + e);
        }

        if(usePolicyFromS3) {
            jspWikiConfig.setProperty("jspwiki.policy.file", policyConfigFile.getName());// see: AuthenticationManager.findConfigFile( engine, policyFileName );
            LOG.info("JSPWiki policy configuration file(" + (policyConfigFile.exists() ? "exists" : "NOT EXISTS") + ", " + (policyConfigFile.canRead() ? "can read" : "CAN'T READ") + "): " + policyConfigFile.getAbsolutePath());
        }
    }

    private static void saveProperties(Properties jspWikiConfig, String rootPath, String env) throws IOException {
        File jspWikiConfigFile = new File(rootPath,"/WEB-INF/jspwiki-custom.properties");
        Iterator<String> it = ConfigurationManager.getConfigInstance().getKeys("jspwiki");
        while(it.hasNext()){
            String key = it.next();
            DynamicStringProperty value = DynamicPropertyFactory.getInstance().getStringProperty(key, null);
            jspWikiConfig.setProperty(key, value.get());
            LOG.debug(key + "=" + value.get());
        }

        try(OutputStream os = new FileOutputStream(jspWikiConfigFile)){
            jspWikiConfig.store(os, "Autogenerated custom configuration file");
        }
        System.setProperty("jspwiki.custom.config", jspWikiConfigFile.getAbsolutePath());
        LOG.info("JSPWiki custom configuration file(" + (jspWikiConfigFile.exists() ? "exists":"NOT EXISTS") + ", " + (jspWikiConfigFile.canRead() ? "can read":"CAN'T READ") + "): " + jspWikiConfigFile.getAbsolutePath());
    }

}
