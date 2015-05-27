package org.hogedriven.s3fx.client;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author irof
 */
public class FixBucketClient implements S3Wrapper {
    private final AmazonS3 client;
    private final String bucketName;

    public FixBucketClient(AmazonS3 client, String bucketName) {
        this.client = client;
        this.bucketName = bucketName;
    }

    @Override
    public List<Bucket> listBuckets() {
        return Collections.singletonList(new Bucket(bucketName));
    }

    @Override
    public Bucket createBucket(String bucketName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteBucket(Bucket bucket) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putObject(Bucket bucket, String key, File srcFile) {
        client.putObject(bucketName, key, srcFile);
    }

    @Override
    public void deleteObject(S3ObjectSummary summary) {
        client.deleteObject(summary.getBucketName(), summary.getKey());
    }

    @Override
    public List<S3ObjectSummary> listObjects(Bucket bucket) {
        List<S3ObjectSummary> objects = new ArrayList<>();
        ObjectListing listing = client.listObjects(bucketName);
        do {
            objects.addAll(listing.getObjectSummaries());
            listing = client.listNextBatchOfObjects(listing);
        } while (listing.getMarker() != null);
        return objects;
    }

    @Override
    public S3Object getObject(S3ObjectSummary summary) {
        return client.getObject(summary.getBucketName(), summary.getKey());
    }

    @Override
    public void getObject(S3ObjectSummary summary, File destFile) {
        client.getObject(new GetObjectRequest(summary.getBucketName(), summary.getKey()), destFile);
    }

    @Override
    public ObjectMetadata getObjectMetadata(S3ObjectSummary summary) {
        return client.getObjectMetadata(summary.getBucketName(), summary.getKey());
    }
}
