package org.hogedriven.s3fx.client;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author irof
 */
public class AmazonS3Builder {

    private AWSCredentials credentials;

    private Consumer<AmazonS3> verifier = client -> {
        // デフォルトは何もしない
    };

    private boolean readOnly = false;
    private String fixBucket;

    private final ClientConfiguration config = new ClientConfiguration();

    public S3Adapter build() {
        // とりあえずタイムアウトなしにしとく
        config.withConnectionTimeout(0).withSocketTimeout(0);

        AmazonS3 client = createAmazonS3();
        verifier.accept(client);

        return createProxy(client);
    }

    private S3Adapter createProxy(AmazonS3 client) {
        return (S3Adapter) Proxy.newProxyInstance(
                ClassLoader.getSystemClassLoader(), new Class[]{S3Adapter.class},
                handling(createAdapter(client)));
    }

    private S3Adapter createAdapter(AmazonS3 client) {
        return (fixBucket != null && !fixBucket.isEmpty()) ?
                new SingleBucketS3Adapter(client, fixBucket) : new S3AdapterImpl(client);
    }

    private AmazonS3 createAmazonS3() {
        return new AmazonS3Client(credentials, config);
    }

    private InvocationHandler handling(S3Adapter s3Adapter) {
        return (proxy, method, args) -> {
            if (readOnly && method.isAnnotationPresent(Bang.class))
                throw new IllegalStateException("ちゃいるどろっくなう");
            return method.invoke(s3Adapter, args);
        };
    }

    /**
     * プロキシの設定
     *
     * @param proxyText プロキシ設定（フォーマット "host:port"）
     */
    public AmazonS3Builder withProxy(String proxyText) {
        Pattern pattern = Pattern.compile("(.+):(\\d+)");
        Matcher matcher = pattern.matcher(proxyText);
        if (matcher.matches()) {
            config.setProxyHost(matcher.group(1));
            config.setProxyPort(Integer.valueOf(matcher.group(2)));
        }
        return this;
    }

    public AmazonS3Builder verify(Consumer<AmazonS3> verifier) {
        this.verifier = verifier;
        return this;
    }

    public AmazonS3Builder basic(String accessKey, String secretKey) {
        credentials = new BasicAWSCredentials(accessKey, secretKey);
        return this;
    }

    public AmazonS3Builder defaultProfile() {
        credentials = new ProfileCredentialsProvider().getCredentials();
        return this;
    }

    public AmazonS3Builder readOnlyLock(boolean flg) {
        this.readOnly = flg;
        return this;
    }

    public AmazonS3Builder fixBucket(String fixBucket) {
        this.fixBucket = fixBucket;
        return this;
    }
}
