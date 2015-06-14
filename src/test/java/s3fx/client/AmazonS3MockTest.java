package s3fx.client;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
    private AmazonS3 mock;

    @Before
    public void setup() throws IOException {
        tempS3.newFolder("bucket1");
        tempS3.newFolder("bucket2");
        tempS3.newFile("bucket1/hoge.txt");
        tempS3.newFile("bucket1/fuga.txt");

        this.mock = AmazonS3Mock.createLocalFilesystemMock(tempS3.getRoot().getAbsolutePath());
    }

    @Test
    public void listBuckets() throws Exception {
        List<Bucket> buckets = mock.listBuckets();
        assertThat(buckets, hasSize(2));
    }

    @Test
    public void listObjects() throws Exception {
        ObjectListing listing = mock.listObjects("bucket1");
        assertThat(listing.getObjectSummaries(), hasSize(2));
    }
}