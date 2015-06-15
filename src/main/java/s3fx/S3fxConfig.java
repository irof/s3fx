package s3fx;

import s3fx.client.S3AdapterBuilder;

/**
 * @author irof
 */
public class S3fxConfig {
    final S3AdapterBuilder adapterBuilder;
    final Class<?> client;

    public S3fxConfig(S3AdapterBuilder adapterBuilder, Class<?> client) {
        this.adapterBuilder = adapterBuilder;
        this.client = client;
    }
}
