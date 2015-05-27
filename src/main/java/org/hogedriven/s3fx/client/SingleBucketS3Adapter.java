package org.hogedriven.s3fx.client;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;

import java.util.Collections;
import java.util.List;

/**
 * @author irof
 */
public class SingleBucketS3Adapter extends S3AdapterImpl {
    private final String bucketName;

    public SingleBucketS3Adapter(AmazonS3 client, String bucketName) {
        super(client);
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
}
