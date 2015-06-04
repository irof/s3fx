package org.hogedriven.s3fx;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.hogedriven.s3fx.client.S3Adapter;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

/**
 * @author irof
 */
public class S3ObjectController implements Initializable {
    public Label bucket;
    public Label key;
    public Label contentType;
    public Label size;
    public Label eTag;
    public Label lastModified;
    public Button observationButton;
    public TextArea observationWindow;

    private final Stage stage;
    private final S3Adapter client;
    private final S3ObjectSummary summary;
    public ComboBox<Charset> observationCharset;
    public Slider observationSize;
    public ProgressIndicator indicator;

    public S3ObjectController(Stage stage, S3Adapter client, S3ObjectSummary summary) {
        this.stage = stage;
        this.client = client;
        this.summary = summary;
    }

    public void onObservation() throws Exception {
        try (S3Object object = client.getObject(summary);
             S3ObjectInputStream content = object.getObjectContent()) {
            byte[] bytes = new byte[(int) observationSize.getValue()];
            content.read(bytes);
            String text = new String(bytes, observationCharset.getValue());
            observationWindow.setText(text);
        }
    }

    public void onDownload() {
        // タイトルを戻すために一旦退避する
        String title = stage.getTitle();
        try {
            DirectoryChooser chooser = new DirectoryChooser();
            stage.setTitle("保存先ディレクトリを選ぶのです");
            File dir = chooser.showDialog(stage);
            if (dir != null) {
                Service<Void> service = downloadService(dir);
                indicator.visibleProperty().unbind();
                indicator.visibleProperty().bind(service.runningProperty());
                service.start();
            }
        } finally {
            stage.setTitle(title);
        }
    }

    private Service<Void> downloadService(File dir) {
        File destFile = new File(dir, new File(key.getText()).getName());
        // 上書きはしない
        if (destFile.exists()) {
            throw new UnsupportedOperationException("同じ名前のファイルがあるよ");
        }
        return new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        client.getObject(summary, destFile);
                        return null;
                    }
                };
            }
        };
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ObjectMetadata meta = client.getObjectMetadata(summary);
        bucket.setText(summary.getBucketName());
        key.setText(summary.getKey());
        contentType.setText(meta.getContentType());
        size.setText(String.format("%,3d byte", summary.getSize()));
        eTag.setText(summary.getETag());
        lastModified.setText(summary.getLastModified().toString());

        observationCharset.setItems(FXCollections.observableArrayList(Charset.availableCharsets().values()));
        observationCharset.setValue(StandardCharsets.UTF_8);

        if (summary.getSize() < 1024) {
            observationSize.setValue(summary.getSize());
            observationSize.setDisable(true);
        }

        String contentType = meta.getContentType();
        boolean isText = contentType != null && contentType.startsWith("text");
        observationButton.setDisable(!isText);
        observationWindow.setDisable(!isText);
    }
}
