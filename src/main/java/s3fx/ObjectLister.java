package s3fx;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import s3fx.client.S3Adapter;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.function.Supplier;

/**
 * @author irof
 */
public class ObjectLister implements Initializable {
    private final Stage stage;
    private final S3Adapter client;

    public TextField bucket;
    public TableView<S3ObjectSummary> objects;
    public TableColumn<S3ObjectSummary, String> key;
    public TableColumn<S3ObjectSummary, Date> time;

    private final Service<ObservableList<S3ObjectSummary>> service;
    public ProgressIndicator indicator;

    public static URL getFXML() {
        return ObjectLister.class.getResource("objectLister.fxml");
    }

    public ObjectLister(Stage stage, S3Adapter client) {
        this.stage = stage;
        this.client = client;

        this.service = createS3Service(() ->
                FXCollections.observableArrayList(client.listObjects(new Bucket(bucket.getText()), "")));
    }

    private <T> Service<T> createS3Service(Supplier<T> task) {
        return new Service<T>() {
            @Override
            protected Task<T> createTask() {
                return new Task<T>() {
                    @Override
                    protected T call() throws Exception {
                        return task.get();
                    }
                };
            }
        };
    }

    public void refresh() {
        service.restart();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        indicator.visibleProperty().bind(service.runningProperty());
        objects.visibleProperty().bind(Bindings.not(service.runningProperty()));
        objects.itemsProperty().bind(service.valueProperty());

        key.setCellValueFactory(new PropertyValueFactory<>("key"));
        time.setCellValueFactory(new PropertyValueFactory<>("lastModified"));
        time.setCellFactory(DateCell::new);
    }

    private static class DateCell extends TableCell<S3ObjectSummary, Date> {
        DateCell(TableColumn<S3ObjectSummary, Date> data) {
        }

        @Override
        protected void updateItem(Date item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty ? "" : new SimpleDateFormat("MM/dd HH:mm:ss").format(item));
        }
    }
}
