package io.ghap.data.contribution;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class S3Utils {

    public static void retrieveMetadata(AmazonS3Client s3, String bucketName, List<Submission> submissions) throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        final List<Future<Submission>> futures = new ArrayList<>(submissions.size());
        for(Submission submission:submissions){
            futures.add(executor.submit(() -> {
                ObjectMetadata meta = s3.getObjectMetadata(bucketName, submission.getKeyName());
                Map<String, String> userMeta = meta.getUserMetadata();
                submission.setUserName(userMeta.get("username"));
                return null;
            }));
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }
}
