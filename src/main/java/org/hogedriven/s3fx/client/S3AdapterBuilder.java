package org.hogedriven.s3fx.client;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.Properties;
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
    public final Property<String> accessKey;
    public final Property<String> secretKey;

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
    public Property<Boolean> readOnly;

    public final Property<String> proxy;

    /**
     * bucket固定設定。
     * 1つのバケットだけ操作する時に。
     */
    public final Property<String> bucket;

    public final Property<Boolean> mockMode = new SimpleBooleanProperty();
    public final Property<Boolean> basicMode = new SimpleBooleanProperty();

    /**
     * プロパティから初期値をロードするコンストラクタ
     *
     * @param properties プロパティ
     */
    public S3AdapterBuilder(Properties properties) {
        accessKey = new SimpleStringProperty(properties.getProperty("accessKey", ""));
        secretKey = new SimpleStringProperty(properties.getProperty("secretKey", ""));
        proxy = new SimpleStringProperty(properties.getProperty("proxy", ""));
        bucket = new SimpleStringProperty(properties.getProperty("bucket", ""));
        readOnly = new SimpleBooleanProperty(Boolean.valueOf(properties.getProperty("readOnly", "true")));
    }

    public S3Adapter build() {
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
        return bucket.getValue().isEmpty() ?
                new S3AdapterImpl(client) :
                new SingleBucketS3Adapter(client, bucket.getValue());
    }

    private AmazonS3 createAmazonS3() {
        if (mockMode.getValue()) {
            return AmazonS3Mock.createMock();
        }
        return new AmazonS3Client(createCredentials(), createConfig());
    }

    private InvocationHandler handling(S3Adapter s3Adapter) {
        return (proxy, method, args) -> {
            if (readOnly.getValue() && method.isAnnotationPresent(Bang.class))
                throw new IllegalStateException("ちゃいるどろっくなう");
            return method.invoke(s3Adapter, args);
        };
    }

    /**
     * タイムアウトの設定: 無制限
     * プロキシの設定: 入力 フォーマット "host:port"）
     */
    private ClientConfiguration createConfig() {
        ClientConfiguration config = new ClientConfiguration();
        // とりあえずタイムアウトなしにしとく
        config.withConnectionTimeout(0).withSocketTimeout(0);

        Pattern pattern = Pattern.compile("(.+):(\\d+)");
        Matcher matcher = pattern.matcher(proxy.getValue());
        if (matcher.matches()) {
            config.setProxyHost(matcher.group(1));
            config.setProxyPort(Integer.valueOf(matcher.group(2)));
        }
        return config;
    }

    public S3AdapterBuilder verifyIf(boolean isVerify, Consumer<AmazonS3> verifier) {
        if (isVerify) {
            this.verifier = verifier;
        }
        return this;
    }

    private AWSCredentials createCredentials() {
        if (basicMode.getValue()) {
            return new BasicAWSCredentials(accessKey.getValue(), secretKey.getValue());
        }
        return new ProfileCredentialsProvider().getCredentials();
    }
}
