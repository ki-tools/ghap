package io.ghap.project.dao.impl;


import com.google.common.base.Stopwatch;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.annotations.Configuration;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import io.ghap.project.dao.StashProjectDao;
import io.ghap.project.domain.*;
import io.ghap.project.model.Commit;
import io.ghap.project.model.StashError;
import io.ghap.project.model.StashException;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.*;

public class DefaultStashProjectDao implements StashProjectDao {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static Integer DEFAULT_PAGE_SIZE = 25;
    @Configuration("stash.login")
    private String login;
    @Configuration("stash.password")
    private String password;
    @Configuration("stash.page.size")
    private String pageSize;


    @Override
    public Set<StashProject> getProjects() throws StashException {
        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        WebResource webResource = client
                .resource(getUrl() + "/projects");
        Integer start = 0;
        Boolean isLastPage = false;
        Set<StashProject> stashProjects = new HashSet<>();
        while (!isLastPage) {
            ClientResponse clientResponse = webResource
                    .queryParam("start", start.toString())
                    .queryParam("limit", getPageSize().toString())
                    .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            checkResponse(clientResponse);
            StashProjectList projectList = clientResponse.getEntity(StashProjectList.class);
            stashProjects.addAll(projectList.values);
            isLastPage = projectList.isLastPage;
            start += getPageSize();
        }
        return stashProjects;
    }

    @Override
    public StashProject getProject(String key) throws StashException {
        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(key));
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        checkResponse(clientResponse);
        StashProject stashProject = clientResponse.getEntity(StashProject.class);
        return stashProject;

    }

    @Override
    public StashProject createProject(StashProject stashProject) throws StashException {
        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        WebResource webResource = client
                .resource(getUrl() + "/projects");
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).post(ClientResponse.class, stashProject);
        checkResponse(clientResponse);
        return clientResponse.getEntity(StashProject.class);
    }

    @Override
    public StashProject updateProject(StashProject stashProject) throws StashException {
        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(stashProject.getKey()));
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).put(ClientResponse.class, stashProject);
        checkResponse(clientResponse);
        return clientResponse.getEntity(StashProject.class);
    }

    @Override
    public void deleteProject(String key) throws StashException {
        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(key));
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).delete(ClientResponse.class);
        checkResponse(clientResponse);
    }

    @Override
    public Set<StashRepo> getReposByProject(String projectKey) throws StashException {
        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(projectKey) + "/repos");
        Integer start = 0;
        Boolean isLastPage = false;
        Set<StashRepo> stashRepos = new HashSet<>();
        while (!isLastPage) {
            ClientResponse clientResponse = webResource
                    .queryParam("start", start.toString())
                    .queryParam("limit", getPageSize().toString())
                    .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            checkResponse(clientResponse);
            StashReposList stashReposList = clientResponse.getEntity(StashReposList.class);
            stashRepos.addAll(stashReposList.values);
            isLastPage = stashReposList.isLastPage;
            start += getPageSize();
        }
        return stashRepos;
    }

    @Override
    public StashRepo getRepo(String projectKey, String slug) throws StashException {
        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(projectKey) + "/repos/" + urlEncode(slug));
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        checkResponse(clientResponse);
        StashRepo stashRepo = clientResponse.getEntity(StashRepo.class);
        return stashRepo;
    }

    @Override
    public StashRepo createRepo(String projectKey, StashRepo stashRepo) throws StashException {
        stashRepo.setSlug(stashRepo.getName());
        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(projectKey) + "/repos");
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).post(ClientResponse.class, stashRepo);
        checkResponse(clientResponse);
        return clientResponse.getEntity(StashRepo.class);
    }

    @Override
    public StashRepo updateRepo(String projectKey, StashRepo stashRepo) throws StashException {
        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(projectKey) + "/repos/" + urlEncode(stashRepo.getSlug()));
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).put(ClientResponse.class, stashRepo);
        checkResponse(clientResponse);
        return clientResponse.getEntity(StashRepo.class);
    }

    @Override
    public void deleteRepo(String projectKey, String slug) throws StashException {
        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(projectKey) + "/repos/" + urlEncode(slug));
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).delete(ClientResponse.class);
        checkResponse(clientResponse);
    }

    @Override
    public void grantPermissionsByProject(Permission permission, String permissions) throws StashException {
        Project project = permission.getProject();
        User user = permission.getUser();

        String projectKey = project.getExternalId();
        String userName = user.getLogin();

        String target = user.getStashTarget();

        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        client.addFilter(new LoggingFilter(System.out));

        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(projectKey) + "/permissions/" + target)
                .queryParam("name", userName)
                .queryParam("permission", permissions);

        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).put(ClientResponse.class);
        checkResponse(clientResponse);
    }

    @Override
    public void revokePermissionsByProject(Permission permission) throws StashException {
        Project project = permission.getProject();
        User user = permission.getUser();

        String projectKey = project.getExternalId();
        String userName = user.getLogin();

        String target = user.getStashTarget();

        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(projectKey) + "/permissions/" + target).queryParam("name", userName);
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).delete(ClientResponse.class);
    }

    @Override
    public void grantPermissionsByRepo(Permission permission, String permissions) throws StashException {
        Repo repo = permission.getRepo();
        User user = permission.getUser();

        String projectKey = repo.getProject().getExternalId();
        String slug = repo.getExternalId();
        String userName = user.getLogin();

        String target = user.getStashTarget();

        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(projectKey) + "/repos/" + urlEncode(slug) + "/permissions/" + target).queryParam("name", userName).queryParam("permission", permissions);
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).put(ClientResponse.class);
        checkResponse(clientResponse);
    }

    @Override
    public void revokePermissionsByRepo(Permission permission) throws StashException {
        Repo repo = permission.getRepo();
        User user = permission.getUser();

        String projectKey = repo.getProject().getExternalId();
        String slug = repo.getExternalId();
        String userName = user.getLogin();

        String target = user.getStashTarget();

        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(projectKey) + "/repos/" + urlEncode(slug) + "/permissions/" + target).queryParam("name", userName);
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).delete(ClientResponse.class);
    }

    @Override
    public void updatePermission(Permission permission) throws StashException {
        if (permission.getProject() != null) {
            revokePermissionsByProject(permission);
            if (permission.getPermissions() > 1) {
                grantPermissionsByProject(permission, "PROJECT_WRITE");
            } else if (permission.getPermissions() == 1) {
                grantPermissionsByProject(permission, "PROJECT_READ");
            }
        } else if (permission.getRepo() != null) {
            revokePermissionsByRepo(permission);
            if (permission.getPermissions() > 1) {
                grantPermissionsByRepo(permission, "REPO_WRITE");
            } else if (permission.getPermissions() == 1) {
                grantPermissionsByRepo(permission, "REPO_READ");
            }
        }

    }

    @Override
    public List<Commit> getHistoryByRepo(String projectKey, String slug) throws StashException {
        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(projectKey) + "/repos/" + urlEncode(slug) + "/commits").queryParam("until", "master");
        Boolean isLastPage = false;
        Integer start = 0;
        List<Commit> commits = new ArrayList<>();
        while (!isLastPage) {
            ClientResponse clientResponse = webResource
                    .queryParam("start", start.toString())
                    .queryParam("limit", getPageSize().toString())
                    .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            if (clientResponse.getStatus() == 404) {
                log.info("commit empty for repo " + projectKey + " repo " + slug);
                break;
            }
            checkResponse(clientResponse);
            CommitsList commitsList = clientResponse.getEntity(CommitsList.class);
            commits.addAll(commitsList.values);
            isLastPage = commitsList.isLastPage;
            start += getPageSize();
        }
        return commits;
    }

    @Override
    public Set<StashPermission> getPermissionsByProject(String userName, String projectKey) throws StashException {
       return getPermissionsByProject(userName, projectKey, ObjectClass.user.getStashTarget());
    }

    @Override
    public Set<StashPermission> getPermissionsByProject(String userName, String projectKey, String groupOrUser) throws StashException {
        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        client.addFilter(new LoggingFilter(System.out));

        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(projectKey) + "/permissions/" + groupOrUser);
        if (StringUtils.isNotBlank(userName)) {
            webResource = webResource.queryParam("filter", userName);
        }
        Integer start = 0;
        Boolean isLastPage = false;
        Set<StashPermission> permissions = new HashSet<>();
        while (!isLastPage) {
            ClientResponse clientResponse = webResource
                    .queryParam("start", start.toString())
                    .queryParam("limit", getPageSize().toString())
                    .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            checkResponse(clientResponse);
            StashUsersList stashReposList = clientResponse.getEntity(StashUsersList.class);
            permissions.addAll(stashReposList.getValues());
            isLastPage = stashReposList.isLastPage();
            start += getPageSize();
        }
        return permissions;
    }

    @Override
    public Set<StashPermission> getPermissionsByRepo(String userName, String projectKey, String repoSlug) throws StashException {
        return getPermissionsByRepo(userName, projectKey, repoSlug, ObjectClass.user.getStashTarget());
    }

    @Override
    public Set<StashPermission> getPermissionsByRepo(String userName, String projectKey, String repoSlug, String groupOrUser) throws StashException {
        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        client.addFilter(new LoggingFilter(System.out));

        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(projectKey) + "/repos/" + urlEncode(repoSlug) + "/permissions/" + groupOrUser);
        if (StringUtils.isNotBlank(userName)) {
            webResource = webResource.queryParam("filter", userName);
        }
        Integer start = 0;
        Boolean isLastPage = false;
        Set<StashPermission> permissions = new HashSet<>();
        while (!isLastPage) {
            ClientResponse clientResponse = webResource
                    .queryParam("start", start.toString())
                    .queryParam("limit", getPageSize().toString())
                    .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            checkResponse(clientResponse);
            StashUsersList stashReposList = clientResponse.getEntity(StashUsersList.class);
            permissions.addAll(stashReposList.getValues());
            isLastPage = stashReposList.isLastPage();
            start += getPageSize();
        }
        return permissions;
    }

    @Override
    public String getLogin() {
        return login;
    }

    public String getUrl() {
        return ConfigurationManager.getConfigInstance().getString("stash.url");
    }

    public String getUrlForFiles() {
        String url = ConfigurationManager.getConfigInstance().getString("stash.url");
        int rest = url.indexOf("rest");
        if (rest > 0) {
            url = url.substring(0, rest);
        }
        return url + "projects/%s/repos/%s/browse/%s";
    }

    @Override
    public void ping() throws StashException {
        Client client = Client.create(getConfig());
        WebResource webResource = client
                .resource(getUrl() + "/application-properties");
        ClientResponse clientResponse = webResource.get(ClientResponse.class);
        checkResponse(clientResponse);
    }

    @Override
    public boolean isFileExistsInStash(String fileName) throws StashException {
        Map<String, String> results =  isFileExistsInStash(Collections.singleton(fileName));
        return results.containsKey(fileName) && results.get(fileName).endsWith(fileName);
    }

    @Override
    public Map<String, String> isFileExistsInStash(final Set<String> fileNames) throws StashException {
        Map<String, String> gitMatches = new HashMap<>(fileNames.size());
        Set<StashProject> projects = getProjects();

        ExecutorService pool = Executors.newCachedThreadPool();
        final String urlForFiles = getUrlForFiles();

        try {
            Collection<Future<Map<String, String>>> results = new ArrayList<>();

            for (Iterator<StashProject> projectIt = projects.iterator(); projectIt.hasNext(); ) {
                final StashProject project = projectIt.next();
                Set<StashRepo> repos = getReposByProject(project.getKey());
                for (Iterator<StashRepo> it = repos.iterator(); it.hasNext(); ) {
                    final StashRepo repo = it.next();

                    results.add(pool.submit(new Callable<Map<String, String>>() {
                        @Override
                        public Map<String, String> call() throws Exception {
                            Map<String, String> partialResults= new HashMap<>();
                            try {
                                Set<String> files = getFiles(project.getKey(), repo.getSlug(), null, 1000);
                                for (String stashFile : files) {
                                    for (String fileName : fileNames) {
                                        if (stashFile.endsWith(fileName)) {
                                            partialResults.put(fileName,
                                                    String.format(urlForFiles, urlEncode(project.getKey()), urlEncode(repo.getSlug()), stashFile));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                String key = (project != null) ? project.getKey() : "unknown";
                                String slug = (repo != null) ? repo.getSlug() : "unknown";
                                log.error("Cannot check file \"" + fileNames + "\" existence in Stash " +
                                        "(project: \"" + key + "\", slug: \"" + slug + "\") " +
                                        "due to the error." + e);
                            }
                            return partialResults;
                        }
                    }));

                }
            }

            for (Future<Map<String, String>> result : results) {
                gitMatches.putAll(result.get());
                boolean hasFalse = false;
                for (String key : fileNames) {
                    if (!gitMatches.containsKey(key)) {
                        hasFalse = true;
                        break;
                    }
                }
                if( !hasFalse ){
                    break;//all files exists and no need to wait all other threads
                }
            }
        } catch (InterruptedException e){
            //ignore
        } catch (ExecutionException e){
            Throwable t = e.getCause();
            if(t instanceof StashException){
                throw (StashException)t;
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            pool.shutdownNow();
        }
        return gitMatches;
    }

    public Set<String> getFiles(String projectKey, String repoSlug, String path) throws StashException {
        return getFiles(projectKey, repoSlug, path, getPageSize());
    }
    public Set<String> getFiles(String projectKey, String repoSlug, String path, Integer pageSize) throws StashException {
        Client client = Client.create(getConfig());
        client.addFilter(new HTTPBasicAuthFilter(login, password));
        if(pageSize == null){
            pageSize = getPageSize();
        }

        WebResource webResource = client
                .resource(getUrl() + "/projects/" + urlEncode(projectKey) + "/repos/" + urlEncode(repoSlug) + "/files" +  (path == null ? "" : "/" + path));
        Integer start = 0;
        Boolean isLastPage = false;
        Set<String> result = new HashSet<>();

        final Stopwatch stopwatch = new Stopwatch().start();

        try {
            while (!isLastPage) {
                ClientResponse clientResponse = webResource
                        .queryParam("start", start.toString())
                        .queryParam("limit", pageSize.toString())
                        .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
                try {
                    checkResponse(clientResponse);
                } catch (StashException e) {
                    if (e.getCode() == HttpServletResponse.SC_NOT_FOUND) {
                        log.error("Cannot get files from project \"" + projectKey + "\", repo \"" + repoSlug + "\", path \"" + path + "\"");
                        break;
                    } else {
                        throw e;
                    }
                }
                StashFileList files = clientResponse.getEntity(StashFileList.class);
                result.addAll(files.getValues());
                isLastPage = files.getIsLastPage();
                start += getPageSize();
            }
        }finally {
            log.info("Get files operation for \"" + projectKey + "." + repoSlug + "\" with path \"" + path + "\" took " + stopwatch.stop());
        }
        return result;
    }

    private void checkResponse(ClientResponse clientResponse) throws StashException {
        if (clientResponse.getStatus() >= 400 && clientResponse.getStatus() <= 500) {

            //something wrong
            log.error("get response from stash status {}", clientResponse.getStatus());

            StashException stashException = clientResponse.getEntity(StashException.class);
            if (stashException != null) {
                log.error("exception code = {}", stashException.getCode());
                if (stashException.getErrors() != null) {
                    for (StashError err : stashException.getErrors()) {
                        log.error("stash error " + err.getMessage() + ", " + err.getExceptionName());
                    }
                }
            }
            stashException.setCode(clientResponse.getStatus());
            throw stashException;
        }
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

    private String urlEncode(String source) {
        try {
            return URLEncoder.encode(source, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private Integer getPageSize() {
        return pageSize == null ? DEFAULT_PAGE_SIZE : Integer.valueOf(pageSize);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StashProjectList {

        private Integer size;
        private Integer limit;
        private Boolean isLastPage;
        private Set<StashProject> values;

        public StashProjectList() {
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }


        public Boolean getIsLastPage() {
            return isLastPage;
        }

        public void setIsLastPage(Boolean isLastPage) {
            this.isLastPage = isLastPage;
        }

        public Set<StashProject> getValues() {
            return values;
        }

        public void setValues(Set<StashProject> values) {
            this.values = values;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StashReposList {
        private Integer size;
        private Integer limit;
        private Boolean isLastPage;
        private Set<StashRepo> values;

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        public Boolean getIsLastPage() {
            return isLastPage;
        }

        public void setIsLastPage(Boolean isLastPage) {
            this.isLastPage = isLastPage;
        }

        public Set<StashRepo> getValues() {
            return values;
        }

        public void setValues(Set<StashRepo> values) {
            this.values = values;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommitsList {
        private Integer size;
        private Integer limit;
        private Boolean isLastPage;
        private Set<Commit> values;

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        public Boolean getIsLastPage() {
            return isLastPage;
        }

        public void setIsLastPage(Boolean isLastPage) {
            this.isLastPage = isLastPage;
        }

        public Set<Commit> getValues() {
            return values;
        }

        public void setValues(Set<Commit> values) {
            this.values = values;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StashUsersList {

        private Integer size;
        private Integer limit;
        private Boolean isLastPage;
        private Set<StashPermission> values;

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        public Boolean isLastPage() {
            return isLastPage;
        }

        public void setIsLastPage(Boolean isLastPage) {
            this.isLastPage = isLastPage;
        }

        public Set<StashPermission> getValues() {
            return values;
        }

        public void setValues(Set<StashPermission> values) {
            this.values = values;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StashFileList {

        private Integer size;
        private Integer limit;
        private Boolean isLastPage;
        private Set<String> values;

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        public Boolean getIsLastPage() {
            return isLastPage;
        }

        public void setIsLastPage(Boolean isLastPage) {
            this.isLastPage = isLastPage;
        }

        public Set<String> getValues() {
            return values;
        }

        public void setValues(Set<String> values) {
            this.values = values;
        }
    }

}
