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
public class ListerController implements Initializable {
    private final Stage stage;
    private final S3Adapter client;

    public TextField bucket1;
    public TableView<S3ObjectSummary> bucket1Table;
    public TextField bucket2;
    public TableView<S3ObjectSummary> bucket2Table;
    public TextField bucket3;
    public TableView<S3ObjectSummary> bucket3Table;
    public TableColumn<S3ObjectSummary, String> bucket1Name;
    public TableColumn<S3ObjectSummary, Date> bucket1Time;
    public TableColumn<S3ObjectSummary, String> bucket2Name;
    public TableColumn<S3ObjectSummary, Date> bucket2Time;
    public TableColumn<S3ObjectSummary, String> bucket3Name;
    public TableColumn<S3ObjectSummary, Date> bucket3Time;

    private final Service<ObservableList<S3ObjectSummary>> bucket1Service;
    private final Service<ObservableList<S3ObjectSummary>> bucket2Service;
    private final Service<ObservableList<S3ObjectSummary>> bucket3Service;
    public ProgressIndicator indicator;

    public ListerController(Stage stage, S3Adapter client) {
        this.stage = stage;
        this.client = client;

        this.bucket1Service = createS3Service(() ->
                FXCollections.observableArrayList(client.listObjects(new Bucket(bucket1.getText()), "")));
        this.bucket2Service = createS3Service(() ->
                FXCollections.observableArrayList(client.listObjects(new Bucket(bucket2.getText()), "")));
        this.bucket3Service = createS3Service(() ->
                FXCollections.observableArrayList(client.listObjects(new Bucket(bucket3.getText()), "")));
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
        bucket3Service.restart();
        bucket1Service.restart();
        bucket2Service.restart();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        indicator.visibleProperty().bind(bucket1Service.runningProperty()
                .or(bucket2Service.runningProperty())
                .or(bucket3Service.runningProperty()));
        setupTable(bucket1Table, bucket1Name, bucket1Time, bucket1Service);
        setupTable(bucket2Table, bucket2Name, bucket2Time, bucket2Service);
        setupTable(bucket3Table, bucket3Name, bucket3Time, bucket3Service);
    }

    private void setupTable(TableView<S3ObjectSummary> table,
                            TableColumn<S3ObjectSummary, String> name,
                            TableColumn<S3ObjectSummary, Date> time,
                            Service<ObservableList<S3ObjectSummary>> service) {
        table.visibleProperty().bind(Bindings.not(service.runningProperty()));
        table.itemsProperty().bind(service.valueProperty());

        name.setCellValueFactory(new PropertyValueFactory<>("key"));
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
