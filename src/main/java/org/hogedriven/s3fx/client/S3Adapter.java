package org.hogedriven.s3fx.client;

import com.amazonaws.services.s3.model.*;

import java.io.File;
import java.util.List;

/**
 * @author irof
 */
public interface S3Adapter {
    List<Bucket> listBuckets();

    @Bang
    Bucket createBucket(String name);

    @Bang
    void deleteBucket(Bucket bucket);

    @Bang
    void putObject(Bucket bucket, String key, File srcFile);

    @Bang
    void deleteObject(S3ObjectSummary summary);

    List<S3ObjectSummary> listObjects(Bucket bucket, String text);

    S3Object getObject(S3ObjectSummary summary);

    void getObject(S3ObjectSummary summary, File destFile);

    ObjectMetadata getObjectMetadata(S3ObjectSummary summary);
}
