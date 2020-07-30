package io.ghap.userdata.contribution.storage.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.model.ObjectListing;
import com.netflix.config.ConfigurationManager;
import com.netflix.governator.annotations.Configuration;
import io.ghap.userdata.contribution.Storage;
import io.ghap.userdata.contribution.storage.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

//import org.apache.commons.io.IOUtils;

@Singleton
public class S3Storage implements Storage {

    private final String PLACEHOLDER_FILE_NAME = "";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Configuration("userdata.s3.bucketname")
    @NotNull
    private String bucket;// default

    @Configuration("userdata.s3.maxConnections")
    private int maxConnections = 50;

    @Configuration("userdata.s3.users.rootPath")
    public String rootPath = "users";// default

    private AmazonS3 s3;

    @Override
    @PostConstruct
    public void init() {
        rootPath = Utils.correctPath(rootPath, true);
        /*
         * AWS credentials provider chain that looks for credentials in this order:
         * Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_KEY
         * Java System Properties - aws.accessKeyId and aws.secretKey
         * Instance profile credentials delivered through the Amazon EC2 metadata service
         */
        AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        ClientConfiguration conf = new ClientConfiguration();
        conf.setMaxConnections( maxConnections );
        s3 = new AmazonS3Client(awsCredentialsProvider, conf);

        Boolean doesBucketExist = null;
        String env = ConfigurationManager.getDeploymentContext().getDeploymentEnvironment();
        if("dev".equals(env)){
            try {
                doesBucketExist = s3.doesBucketExist(bucket);
            }
            catch(Exception e){
                log.error("S3 integration was disabled. See logs.");
            }
        }
        else  {
            doesBucketExist = s3.doesBucketExist(bucket);
        }

        if(doesBucketExist != null && !doesBucketExist)
            throw new AmazonClientException("Bucket " + bucket + " doesn't exist.");
    }

    @Override
    public io.ghap.userdata.contribution.storage.ObjectListing listObjects(String key, boolean recursive) {
        String path = getRootPath() + key;
        ListObjectsRequest request = new ListObjectsRequest().withPrefix(path);
        request.withBucketName(bucket);
        if( !recursive ){
            request.withDelimiter("/");
        }
        ObjectListing objectListing = s3.listObjects(request);

        Collection<FileInfo> files = new ArrayList<>();
        Collection<FileInfo> folders = new ArrayList<>();
        List<S3ObjectSummary> summaries = objectListing.getObjectSummaries();
        for (S3ObjectSummary summary : summaries) {
            if( !summary.getKey().equals(path) ) {
                files.add(new FileInfo(summary.getKey(), summary.getSize(), summary.getLastModified()));
            }
        }
        List<String>     commonPrefixes = objectListing.getCommonPrefixes();
        for(String folder:commonPrefixes){
            folders.add(new FileInfo(folder));
        }
        log.debug("List objects from \"" + path + "\"(recursive: " + recursive + "). Files: " + files.size() + ", folders: " + folders.size());
        return new io.ghap.userdata.contribution.storage.ObjectListing(files, folders);
    }

    @Override
    public void makeFolder(String userName, String path, String name) throws IOException {

        path = Utils.correctPath(path, true);
        name = Utils.correctPath(name, true);

        String key = getRootPath() + path + name + PLACEHOLDER_FILE_NAME;
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(0);

        try(InputStream is = new ByteArrayInputStream(new byte[0])) {
            s3.putObject(new PutObjectRequest(bucket, key, is, meta));

            log.debug("Create folder \"" + key + "\"");
        }
    }

    @Override
    public void deleteObjects(String path) {
        path = getRootPath() + path;
        if(path.endsWith("/")) {
            ObjectListing listObjects = s3.listObjects(bucket, path);
            List<S3ObjectSummary> summaries = listObjects.getObjectSummaries();
            if (summaries == null || summaries.isEmpty()) {
                return;
            }
            List<String> keys = new ArrayList<>(summaries.size());
            for (S3ObjectSummary summary : summaries) {
                keys.add(summary.getKey());
            }
            DeleteObjectsRequest request = new DeleteObjectsRequest(bucket).withKeys(keys.toArray(new String[keys.size()]));
            s3.deleteObjects(request);
        } else {
            DeleteObjectsRequest request = new DeleteObjectsRequest(bucket).withKeys(new String[]{path});
            s3.deleteObjects(request);
        }
    }

    @Override
    public InputStream loadFolder(String path, String root) throws IOException {
        ObjectListing listObjects = s3.listObjects(bucket, getRootPath() + path);
        List<S3ObjectSummary> summaries = listObjects.getObjectSummaries();
        if (summaries == null || summaries.isEmpty()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        final File file = File.createTempFile(UUID.randomUUID().toString(), ".zip");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
        try {
            for (S3ObjectSummary summary : summaries) {
                S3Object object = s3.getObject(bucket, summary.getKey());
                try(S3ObjectInputStream objectStream = object.getObjectContent()) {
                    ;
                    String key = summary.getKey().replace(getRootPath(), "");
                    if (root != null) {
                        key = key.replace(root + "/", "");
                    }
                    ZipEntry zipEntry = new ZipEntry(key);
                    zos.putNextEntry(zipEntry);
                    IOUtils.copy(objectStream, zos);
                }
            }
        } finally {
            zos.flush();
            zos.close();
        }
        return new FileInputStream(file) {
            @Override
            public void close() throws IOException {
                super.close();
                FileUtils.deleteQuietly(file);
            }
        };
    }

    @Override
    public String getRootPath() {
        return rootPath;
    }

    @Override
    public FileStream getFileStream(String keyPath, boolean isAbsolutePath) {
        S3Object s3Object = s3.getObject(bucket, getRootPath() + keyPath);
        ObjectMetadata meta = s3Object.getObjectMetadata();
        return new FileStream(s3Object.getObjectContent(), meta.getContentType(), meta.getContentLength());
    }

    @Override
    public void putObject(String userName, String path, String name, File file) {
        path = Utils.correctPath(path, true);
        name = Utils.correctPath(name, false);

        String key = getRootPath() + path + name;
        s3.putObject(bucket, key, file);
    }

    public boolean isExists(String path, String name, boolean isFolder){
        try {
            path = Utils.correctPath(path, true);
            name = Utils.correctPath(name, isFolder);

            String key = getRootPath() + path + name;

            return s3.getObjectMetadata(bucket, key) != null;
        } catch(AmazonServiceException ex){
            if(ex.getStatusCode() == 404){
                return false;
            }
            throw ex;
        }
    }

    @Override
    public boolean isExists(String key){
        try {
            return s3.getObjectMetadata(bucket, getRootPath() + key) != null;
        } catch(AmazonServiceException ex){
            if(ex.getStatusCode() == 404){
                return false;
            }
            throw ex;
        }
    }

}
