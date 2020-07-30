package io.ghap.service.bean;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import io.ghap.model.ShinyAppDescriptor;
import io.ghap.service.ElasticSearchService;
import io.ghap.service.PrepareRulesService;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author maxim.tulupov@ontarget-group.com
 */
@Singleton
public class PrepareRulesServiceBean implements PrepareRulesService {

    static {
        SSLContext.setDefault(getSSLContext());
    }

    //TODO delete it
    private static final String JSON = "[{\"path\": \"./datainventory/meta-data.json\", \"application\": {\"ApplicationName\": \"Data Inventory\", \"ApplicationRoot\": \"datainventory\", \"Updated\": \"2016-04-14 01:55:04\", \"Description\": \"Inventory of available curated data sets.\", \"Keyword\": [\"inventory\", \"summary\"], \"Grant\": \"N/A\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"SortPriority\": 0, \"Project\": \"N/A\", \"Thumbnail\": \"/thumbnail/datainventory/thumbnail.png\"}}, {\"path\": \"./trelliscopebfzn/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: bfzn\", \"SortPriority\": 10, \"Updated\": \"2016-04-19 17:47:40\", \"Description\": \"Trelliscope displays for Burkina Faso Zn Study, Zn Trial in Burkina Faso (Available in Git at: HBGD/ki1112895)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1112895\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopebfzn\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopebfzn/thumbnail.png\"}}, {\"path\": \"./trelliscopebmbm/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: bmbm\", \"SortPriority\": 12, \"Updated\": \"2016-04-19 17:36:54\", \"Description\": \"Trelliscope displays for BAMBAM Study, Brain Myelination in Children (Available in Git at: HBGD/ki1120016)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1120016\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopebmbm\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopebmbm/thumbnail.png\"}}, {\"path\": \"./trelliscopebngd/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: bngd\", \"SortPriority\": 14, \"Updated\": \"2016-04-19 17:39:14\", \"Description\": \"Trelliscope displays for Bangladesh Diarrhea Study, Longitudinal Growth Study in Bangladesh (Available in Git at: HBGD/ki1112895)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1112895\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopebngd\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopebngd/thumbnail.png\"}}, {\"path\": \"./trelliscopecmc/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: cmc\", \"SortPriority\": 18, \"Updated\": \"2016-04-19 17:54:52\", \"Description\": \"Trelliscope displays for CMC-V-BCS-2002 Study, CMC Vellore Birth Cohort 2002 (Available in Git at: HBGD/ki1000108)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1000108\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopecmc\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopecmc/thumbnail.png\"}}, {\"path\": \"./trelliscopecmin/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: cmin\", \"SortPriority\": 20, \"Updated\": \"2016-04-19 17:57:15\", \"Description\": \"Trelliscope displays for CMIN Study, CMIN Study (Available in Git at: HBGD/ki1114097)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1114097\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopecmin\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopecmin/thumbnail.png\"}}, {\"path\": \"./trelliscopecort/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: cort\", \"SortPriority\": 24, \"Updated\": \"2016-01-21 09:50:30\", \"Description\": \"Trelliscope displays for COHORTS Study, COHORTS (Available in Git at: HBGD/ki1135781)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1135781\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopecort\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopecort/thumbnail.png\"}}, {\"path\": \"./trelliscopecntt/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: cntt\", \"SortPriority\": 24, \"Updated\": \"2016-04-19 21:21:21\", \"Description\": \"Trelliscope displays for CONTENT Study, CONTENT Study (Available in Git at: HBGD/ki1114097)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1114097\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopecntt\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopecntt/thumbnail.png\"}}, {\"path\": \"./trelliscopecpp4/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: cpp4\", \"SortPriority\": 26, \"Updated\": \"2016-01-21 09:58:27\", \"Description\": \"Trelliscope displays for CPP4674 Study, Collaborative Perinatal Project (Available in Git at: HBGD/CPP4674)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"CPP4674\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopecpp4\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopecpp4/thumbnail.png\"}}, {\"path\": \"./trelliscopeee/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: ee\", \"SortPriority\": 30, \"Updated\": \"2016-01-21 10:02:40\", \"Description\": \"Trelliscope displays for EE Study, Study of Biomarkers for EE (Available in Git at: HBGD/ki1000109)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1000109\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopeee\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopeee/thumbnail.png\"}}, {\"path\": \"./trelliscopeeczn/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: eczn\", \"SortPriority\": 36, \"Updated\": \"2016-04-19 22:17:39\", \"Description\": \"Trelliscope displays for Ecuador Zn Study, Ecuador Zn Trial (Available in Git at: HBGD/ki1112895)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1112895\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopeeczn\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopeeczn/thumbnail.png\"}}, {\"path\": \"./trelliscopegems/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: gems\", \"SortPriority\": 36, \"Updated\": \"2016-01-21 11:37:05\", \"Description\": \"Trelliscope displays for GEMS-1 Study, GEMS-1 Study (Available in Git at: HBGD/ki0038874)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki0038874\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopegems\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopegems/thumbnail.png\"}}, {\"path\": \"./trelliscopegrip/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: grip\", \"SortPriority\": 38, \"Updated\": \"2016-01-21 10:02:53\", \"Description\": \"Trelliscope displays for Grip Study, LRTI. RSV and Influenza Cohort Study (Available in Git at: HBGD/ki1000109)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1000109\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopegrip\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopegrip/thumbnail.png\"}}, {\"path\": \"./trelliscopefels/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: fels\", \"SortPriority\": 40, \"Updated\": \"2016-04-19 22:22:29\", \"Description\": \"Trelliscope displays for Fels Study, The Fels Longitudinal Study (Available in Git at: HBGD/ki1135978)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1135978\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopefels\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopefels/thumbnail.png\"}}, {\"path\": \"./trelliscopegbsc/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: gbsc\", \"SortPriority\": 42, \"Updated\": \"2016-04-19 23:02:44\", \"Description\": \"Trelliscope displays for Guatemala BSC Study, Longitudinal study of BSC in Guatemala (Available in Git at: HBGD/ki1112895)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1112895\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopegbsc\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopegbsc/thumbnail.png\"}}, {\"path\": \"./trelliscopejvt3/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: jvt3\", \"SortPriority\": 42, \"Updated\": \"2016-01-21 10:05:36\", \"Description\": \"Trelliscope displays for JiVitA-3 Study, JiVitA JAMA Cohort (Available in Git at: HBGD/kiGH5241)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"kiGH5241\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopejvt3\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopejvt3/thumbnail.png\"}}, {\"path\": \"./trelliscopegmsa/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: gmsa\", \"SortPriority\": 46, \"Updated\": \"2016-04-19 22:47:41\", \"Description\": \"Trelliscope displays for GEMS-1A Study, GEMS-1A Study (Available in Git at: HBGD/ki0038874)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki0038874\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopegmsa\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopegmsa/thumbnail.png\"}}, {\"path\": \"./trelliscopegsto/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: gsto\", \"SortPriority\": 50, \"Updated\": \"2016-04-19 23:05:05\", \"Description\": \"Trelliscope displays for GUSTO Study, GUSTO (Available in Git at: HBGD/ki1126927)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1126927\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopegsto\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopegsto/thumbnail.png\"}}, {\"path\": \"./trelliscopeincp/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: incp\", \"SortPriority\": 54, \"Updated\": \"2016-04-19 23:11:16\", \"Description\": \"Trelliscope displays for INCAP Study, INCAP Next Generation (Available in Git at: HBGD/ki1135782)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1135782\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopeincp\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopeincp/thumbnail.png\"}}, {\"path\": \"./trelliscopeknba/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: knba\", \"SortPriority\": 58, \"Updated\": \"2016-04-19 23:33:19\", \"Description\": \"Trelliscope displays for Keneba Study, MRC Kenaba (Available in Git at: HBGD/ki1101329)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1101329\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopeknba\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopeknba/thumbnail.png\"}}, {\"path\": \"./trelliscopelnsz/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: lnsz\", \"SortPriority\": 60, \"Updated\": \"2016-04-19 23:07:39\", \"Description\": \"Trelliscope displays for iLiNS-Zinc Study, iLiNS-Zinc Study (Available in Git at: HBGD/ki1112895)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1112895\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopelnsz\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopelnsz/thumbnail.png\"}}, {\"path\": \"./trelliscopemled/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: mled\", \"SortPriority\": 62, \"Updated\": \"2016-04-19 23:37:48\", \"Description\": \"Trelliscope displays for MAL-ED Study, MAL-ED Study (Available in Git at: HBGD/ki0047075)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki0047075\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopemled\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopemled/thumbnail.png\"}}, {\"path\": \"./trelliscopemmam/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: mmam\", \"SortPriority\": 64, \"Updated\": \"2016-04-19 23:41:42\", \"Description\": \"Trelliscope displays for MaliMMAM Study, Deuterium dilution study in Mali (Available in Git at: HBGD/ki1000112)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1000112\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopemmam\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopemmam/thumbnail.png\"}}, {\"path\": \"./trelliscopenbrt/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: nbrt\", \"SortPriority\": 72, \"Updated\": \"2016-04-19 23:44:02\", \"Description\": \"Trelliscope displays for NIH-Birth Study, NIH Birth Cohort Study (Available in Git at: HBGD/ki1017093)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1017093\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopenbrt\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopenbrt/thumbnail.png\"}}, {\"path\": \"./trelliscopenpre/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: npre\", \"SortPriority\": 74, \"Updated\": \"2016-04-19 23:46:41\", \"Description\": \"Trelliscope displays for NIH-Preschool Study, NIH Preschool Cohort Study (Available in Git at: HBGD/ki1017093)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1017093\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopenpre\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopenpre/thumbnail.png\"}}, {\"path\": \"./trelliscopephua/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: phua\", \"SortPriority\": 78, \"Updated\": \"2016-04-19 23:49:03\", \"Description\": \"Trelliscope displays for Peru Huascar Study, Infant Growth in Peru (Available in Git at: HBGD/ki1112895)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1112895\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopephua\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopephua/thumbnail.png\"}}, {\"path\": \"./trelliscopepops/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: pops\", \"SortPriority\": 80, \"Updated\": \"2016-04-19 23:56:08\", \"Description\": \"Trelliscope displays for POPS Study,  (Available in Git at: HBGD/ki1000130)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1000130\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopepops\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopepops/thumbnail.png\"}}, {\"path\": \"./trelliscopeppd/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: ppd\", \"SortPriority\": 82, \"Updated\": \"2016-04-19 23:51:23\", \"Description\": \"Trelliscope displays for Peru PersistDiarrhea Study, Peru Persistent Diarrhea (Available in Git at: HBGD/ki1112895)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1112895\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopeppd\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopeppd/thumbnail.png\"}}, {\"path\": \"./trelliscopeprbt/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: prbt\", \"SortPriority\": 84, \"Updated\": \"2016-04-19 23:58:55\", \"Description\": \"Trelliscope displays for PROBIT Study, PROBIT Study (Available in Git at: HBGD/ki1119695)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1119695\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopeprbt\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopeprbt/thumbnail.png\"}}, {\"path\": \"./trelliscopeprvd/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: prvd\", \"SortPriority\": 86, \"Updated\": \"2016-04-20 00:14:06\", \"Description\": \"Trelliscope displays for PROVIDE Study, PROVIDE Study (Available in Git at: HBGD/ki1017093)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1017093\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopeprvd\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopeprvd/thumbnail.png\"}}, {\"path\": \"./trelliscopepzn/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: pzn\", \"SortPriority\": 88, \"Updated\": \"2016-04-19 23:53:44\", \"Description\": \"Trelliscope displays for Peru Zn Study, Peru Zn Fortification (Available in Git at: HBGD/ki1112895)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1112895\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopepzn\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopepzn/thumbnail.png\"}}, {\"path\": \"./trelliscoperspk/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: rspk\", \"SortPriority\": 90, \"Updated\": \"2016-04-20 00:17:00\", \"Description\": \"Trelliscope displays for ResPak Study, Respiratory Pathogens Birth Cohort (Available in Git at: HBGD/ki1000109)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1000109\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscoperspk\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscoperspk/thumbnail.png\"}}, {\"path\": \"./trelliscopesmcc/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: smcc\", \"SortPriority\": 92, \"Updated\": \"2016-04-20 00:19:22\", \"Description\": \"Trelliscope displays for SMOCC Study, Social Medical Social Medical Survey of Children attending Child health Clinics (Available in Git at: HBGD/ki1000130)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1000130\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopesmcc\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopesmcc/thumbnail.png\"}}, {\"path\": \"./trelliscopezvit/meta-data.json\", \"application\": {\"ApplicationName\": \"Trelliscope: zvit\", \"SortPriority\": 94, \"Updated\": \"2016-04-20 00:23:17\", \"Description\": \"Trelliscope displays for ZVITAMBO Study, ZVITAMBO (Available in Git at: HBGD/ki1126311)\", \"Keyword\": [\"trelliscope\"], \"Grant\": \"ki1126311\", \"Author\": \"Ryan Hafen <rhafen@gmail.com>\", \"ApplicationRoot\": \"trelliscopezvit\", \"Project\": \"HBGD\", \"Thumbnail\": \"/thumbnail/trelliscopezvit/thumbnail.png\"}}, {\"path\": \"./maledqpm/meta-data.json\", \"application\": {\"ApplicationName\": \"MAL-ED-qPM\", \"ApplicationRoot\": \"maledqpm\", \"Updated\": \"2016-03-10 14:54:36\", \"Description\": \"An app to Explore MAL-ED for key qPM questions\", \"Keyword\": [\"Exploratory Data Analysis, MAL-ED\"], \"Grant\": \"N/A\", \"Author\": \"Samer Mouksassi\", \"Project\": \"N/A\", \"Thumbnail\": \"/thumbnail/maledqpm/thumbnail.png\"}}, {\"path\": \"./ggplotyourdata/meta-data.json\", \"application\": {\"ApplicationName\": \"GgplotYourData\", \"ApplicationRoot\": \"ggplotyourdata\", \"Updated\": \"2015-12-10 21:55:11\", \"Description\": \"A general purpose app to quickly plot your data\", \"Keyword\": [\"Exploratory Data Analysis\"], \"Grant\": \"N/A\", \"Author\": \"Samer Mouksassi\", \"Project\": \"N/A\", \"Thumbnail\": \"/thumbnail/ggplotyourdata/thumbnail.png\"}}]";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Configuration("shiny.url")
    private String shinyUrl;
    @Configuration("registry.json.reload.period.sec")
    private int reloadPeriod;
    private List<ShinyAppDescriptor> apps;
    private Timer timer = new Timer();
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    @Inject
    private ElasticSearchService elasticSearchService;

    @Override
    public void scheduleRegistryUpdate() {
        timer.scheduleAtFixedRate(new ReloadTask(), 0, reloadPeriod * 1000L);
    }

    @Override
    public List<ShinyAppDescriptor> getRegistryFile() {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        readLock.lock();
        try {
            if (apps == null || apps.isEmpty()) {
                apps = loadRegistryFile();
//                ObjectMapper mapper = new ObjectMapper();
//                mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
//                List<ShinyAppDescriptor> desc = null;
//                try {
//                    desc = mapper.readValue(JSON, new TypeReference<List<ShinyAppDescriptor>>(){});
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                apps = desc;
            }
            return apps;
        } finally {
            readLock.unlock();
        }
    }

    protected List<ShinyAppDescriptor> loadRegistryFile() {
        log.info("start read registry file from the url {}", shinyUrl);
        Client client = create();
        List<ShinyAppDescriptor> descriptors = client.resource(shinyUrl)
                .path("registry.json")
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .get(new GenericType<List<ShinyAppDescriptor>>() {
                });
        if (descriptors != null) {
            for (ShinyAppDescriptor descriptor : descriptors) {
                log.info("descriptor = {}", descriptor);
            }
        } else {
            log.info("descriptors is null");
        }
        try {
            elasticSearchService.putAppsInElasticSearch(descriptors);
        } catch (Exception e) {
            log.error("error put apps in elastic search", e);
        }
        return descriptors;
    }

    public Client create() {
        Client client = Client.create(getConfig());
        return client;
    }

    public static com.sun.jersey.api.client.config.ClientConfig getConfig() {
        com.sun.jersey.api.client.config.ClientConfig config = new com.sun.jersey.api.client.config.DefaultClientConfig(); // SSL configuration
        try {
            config.getProperties().put(com.sun.jersey.client.urlconnection.HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new com.sun.jersey.client.urlconnection.HTTPSProperties(new HostnameVerifier() {

                @Override
                public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                    return true;
                }
            }, getSSLContext()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        config.getClasses().add(JacksonJsonProvider.class);
        return config;
    }

    // disable cert verification
    private static SSLContext getSSLContext() {

        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("SSL");
            ctx.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (java.security.GeneralSecurityException ex) {
        }
        return ctx;
    }

    private class ReloadTask extends TimerTask {

        @Override
        public void run() {
            ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
            writeLock.lock();
            try {
                apps = loadRegistryFile();
            } finally {
                writeLock.unlock();
            }
        }
    }
}
