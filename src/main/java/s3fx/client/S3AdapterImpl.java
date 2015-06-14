package s3fx.client;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;

import java.io.File;
import java.util.List;

/**
 * @author irof
 */
public class S3AdapterImpl implements S3Adapter {
    protected final AmazonS3 client;

    public S3AdapterImpl(AmazonS3 client) {
        this.client = client;
    }

    @Override
    public List<Bucket> listBuckets() {
        return client.listBuckets();
    }

    @Override
    public Bucket createBucket(String bucketName) {
        return client.createBucket(bucketName);
    }

    @Override
    public void deleteBucket(Bucket bucket) {
        client.deleteBucket(bucket.getName());
    }

    @Override
    public void putObject(Bucket bucket, String key, File srcFile) {
        client.putObject(bucket.getName(), key, srcFile);
    }

    @Override
    public void deleteObject(S3ObjectSummary summary) {
        client.deleteObject(summary.getBucketName(), summary.getKey());
    }

    @Override
    public List<S3ObjectSummary> listObjects(Bucket bucket, String prefix) {
        ObjectListing listing = client.listObjects(bucket.getName(), prefix);
        return listing.getObjectSummaries();
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
