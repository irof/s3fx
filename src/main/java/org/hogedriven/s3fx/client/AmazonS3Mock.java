package org.hogedriven.s3fx.client;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author irof
 */
public class AmazonS3Mock {

    public static AmazonS3 createLocalFilesystemMock(String rootPath) {
        Path root = Paths.get(rootPath);
        return createProxy(new AmazonS3Mock() {
            @Override
            protected List<Bucket> listBuckets() throws IOException {
                return Files.list(root)
                        .map(Path::toFile)
                        .filter(File::isDirectory)
                        .map(f -> new Bucket(f.getName()))
                        .collect(toList());
            }

            @Override
            protected Object listObjects(String bucketName) throws Exception {
                ObjectListing listing = new ObjectListing();
                listing.getObjectSummaries().addAll(
                        Files.list(root.resolve(bucketName))
                                .map(Path::toFile)
                                .map(f -> {
                                    S3ObjectSummary summary = new S3ObjectSummary();
                                    summary.setKey(f.getName());
                                    return summary;
                                })
                                .collect(toList()));
                return listing;
            }
        });
    }

    public static AmazonS3 createMock() {
        return createProxy(new AmazonS3Mock());
    }

    private static AmazonS3 createProxy(AmazonS3Mock amazonS3Mock) {
        return (AmazonS3) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),
                new Class[]{AmazonS3.class}, amazonS3Mock.createInvocationHandler());
    }

    private InvocationHandler createInvocationHandler() {
        return (proxy, method, args) -> {
            System.out.printf("invoke: %s %s(%s)%n",
                    method.getReturnType().getSimpleName(), method.getName(), Arrays.toString(args));
            TimeUnit.SECONDS.sleep(3);
            switch (method.getName()) {
                case "listBuckets":
                    return listBuckets();
                case "listObjects":
                    return listObjects((String) args[0]);
                case "getObjectMetadata":
                    return getObjectMetadata();
                case "createBucket":
                    return createBucket((String) args[0]);
                case "getS3AccountOwner":
                    return new Owner("mockId", "mockDisplayName");
                case "listNextBatchOfObjects":
                    return new ObjectListing();
                case "deleteBucket":
                case "putObject":
                case "deleteObject":
                    return null;
            }
            throw new UnsupportedOperationException(method.getName().toString());
        };
    }

    protected Object listObjects(String arg) throws Exception {
        ObjectListing listing = new ObjectListing();
        listing.getObjectSummaries().addAll(Stream.generate(AmazonS3Mock::createS3ObjectSummary)
                .limit(20)
                .collect(toList()));
        return listing;
    }

    protected List<Bucket> listBuckets() throws Exception {
        return Arrays.asList(createBucket("hoge"), createBucket("fuga"), createBucket("piyo"));
    }

    private static ObjectMetadata getObjectMetadata() {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType("text/plane");
        return objectMetadata;
    }

    private static Bucket createBucket(String name) {
        Bucket bucket = new Bucket(name);
        bucket.setCreationDate(new Date());
        bucket.setOwner(new Owner("OwnerIdByMock", "OwnerDisplayNameByMock"));
        return bucket;
    }

    private static S3ObjectSummary createS3ObjectSummary() {
        S3ObjectSummary objectSummary = new S3ObjectSummary();
        objectSummary.setBucketName("S3ObjectSummaryBucketNameByMock");
        objectSummary.setKey("KeyByMock" + UUID.randomUUID());
        objectSummary.setLastModified(new Date());
        objectSummary.setSize(12345678);
        objectSummary.setStorageClass("S3ObjectSummaryStorageClassByMock");
        objectSummary.setETag("DUMMY ETag Value");
        return objectSummary;
    }
}
