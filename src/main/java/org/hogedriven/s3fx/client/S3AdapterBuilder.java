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
public class S3AdapterBuilder {

    /**
     * 認証方式。デフォルトは ~/.aws/credentials を使用する。
     */
    private AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();

    /**
     * 接続後の確認処理。
     * AmazonS3はインスタンス生成時に接続確認を行わないので、認証とかを確認するために何か呼び出してみる。
     */
    private Consumer<AmazonS3> verifier = client -> {
        // デフォルトは何もしない
    };

    /**
     * 読み取り専用設定。
     * AWSの権限があっても、うっかり更新したくない時とか。
     */
    private boolean readOnly = false;

    /**
     * bucket固定設定。
     * 1つのバケットだけ操作する時に。
     */
    private String fixBucket;

    private final ClientConfiguration config = new ClientConfiguration();
    private boolean mock = false;

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
        if (mock) {
            return AmazonS3Mock.createMock();
        }
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
    public S3AdapterBuilder withProxy(String proxyText) {
        Pattern pattern = Pattern.compile("(.+):(\\d+)");
        Matcher matcher = pattern.matcher(proxyText);
        if (matcher.matches()) {
            config.setProxyHost(matcher.group(1));
            config.setProxyPort(Integer.valueOf(matcher.group(2)));
        }
        return this;
    }

    public S3AdapterBuilder verifyIf(boolean isVerify, Consumer<AmazonS3> verifier) {
        if (isVerify) {
            this.verifier = verifier;
        }
        return this;
    }

    public S3AdapterBuilder basicIf(boolean selected, String accessKey, String secretKey) {
        if (selected) {
            credentials = new BasicAWSCredentials(accessKey, secretKey);
        }
        return this;
    }

    public S3AdapterBuilder readOnlyLock(boolean flg) {
        this.readOnly = flg;
        return this;
    }

    public S3AdapterBuilder fixBucket(String fixBucket) {
        this.fixBucket = fixBucket;
        return this;
    }

    public S3AdapterBuilder withMock(boolean mock) {
        this.mock = mock;
        return this;
    }
}
