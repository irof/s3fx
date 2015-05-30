package org.hogedriven.s3fx.client;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * @author irof
 */
public class AmazonS3MockTest {

    @Rule
    public TemporaryFolder tempS3 = new TemporaryFolder();

    @Before
    public void setup() throws IOException {
        tempS3.newFolder("bucket1");
        tempS3.newFolder("bucket2");
    }

    @Test
    public void listBuckets() throws Exception {
        AmazonS3 mock = AmazonS3Mock.createLocalFilesystemMock(tempS3.getRoot().getAbsolutePath());
        List<Bucket> buckets = mock.listBuckets();
        assertThat(buckets, hasSize(2));
    }
}