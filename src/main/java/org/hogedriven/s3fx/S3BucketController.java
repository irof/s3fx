package org.hogedriven.s3fx;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import org.hogedriven.s3fx.client.S3Adapter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;

/**
 * @author irof
 */
public class S3BucketController implements Initializable {
    private final S3Adapter client;

    private final Stage stage;
    private final Map<S3ObjectIdentifier, Stage> objectWindows = new HashMap<>();

    public ComboBox<Bucket> bucket;
    public Button refreshBucketsButton;
    public Button createBucketButton;
    public Button deleteBucketButton;

    public ProgressIndicator progress;
    public Button deleteButton;
    public Button uploadButton;
    public TextField filterText;

    public TableView<S3ObjectSummary> objectList;
    public TableColumn<S3ObjectSummary, String> tableNameColumn;
    public TableColumn<S3ObjectSummary, Long> tableSizeColumn;
    public TableColumn<S3ObjectSummary, Date> tableLastModifiedColumn;

    private final Service<ObservableList<Bucket>> listBucketsService;
    private final Service<ObservableList<S3ObjectSummary>> listObjectsService;

    public S3BucketController(Stage stage, S3Adapter client) {
        this.stage = stage;
        this.client = client;
        this.listBucketsService = createS3Service(() ->
                FXCollections.observableArrayList(client.listBuckets()));
        this.listObjectsService = createS3Service(() ->
                FXCollections.observableArrayList(
                        client.listObjects(bucket.getValue(), filterText.getText())));
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

    public void getBuckets() {
        listBucketsService.restart();
    }

    public void createBucket() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("新しいBucketの作成");
        dialog.setHeaderText("作りたいBucketの名前を入力してください");
        dialog.setContentText("new Bucket Name :");

        dialog.showAndWait().ifPresent(name ->
                bucket.getItems().add(client.createBucket(name)));
    }

    public void deleteBucket() {
        client.deleteBucket(bucket.getValue());
        bucket.getItems().remove(bucket.getValue());
        bucket.getSelectionModel().clearSelection();
    }

    public void uploadFile() {
        // ファイルを選択してもらう
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) return;

        TextInputDialog dialog = new TextInputDialog(file.getName());
        dialog.setTitle("Bucketに格納するキーを入力してください");
        dialog.setHeaderText(file.getPath());
        dialog.setContentText("Key :");

        dialog.showAndWait().ifPresent(name -> {
            client.putObject(bucket.getValue(), name, file);
            listObjectsService.restart();
        });
    }

    public void deleteFile() {
        S3ObjectSummary selectedItem = objectList.getSelectionModel().getSelectedItem();
        client.deleteObject(selectedItem);
        objectList.getItems().remove(selectedItem);
        S3ObjectIdentifier id = new S3ObjectIdentifier(selectedItem);
        if (objectWindows.containsKey(id)) {
            objectWindows.remove(id).close();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // サービス実行中はインジケーター表示
        progress.visibleProperty().bind(listBucketsService.runningProperty()
                .or(listObjectsService.runningProperty()));

        bucket.itemsProperty().bind(listBucketsService.valueProperty());
        bucket.setCellFactory(this::createBucketCell);
        bucket.setConverter(new StringConverter<Bucket>() {
            @Override
            public String toString(Bucket object) {
                return object.getName();
            }

            @Override
            public Bucket fromString(String string) {
                // StringからBucketへの変換はしない
                throw new UnsupportedOperationException();
            }
        });
        bucket.valueProperty().addListener((observable, oldValue, newValue) -> {
            listObjectsService.restart();
        });

        objectList.itemsProperty().bind(listObjectsService.valueProperty());
        objectList.setRowFactory(table -> {
            TableRow<S3ObjectSummary> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                S3ObjectSummary item = row.getItem();
                if (item == null || event.getClickCount() != 2) return;
                S3ObjectIdentifier id = new S3ObjectIdentifier(item);
                if (objectWindows.containsKey(id)) {
                    objectWindows.get(id).requestFocus();
                } else {
                    Stage objectWindow = createS3ObjectWindow(item);
                    // 上を合わせて右に並べて出す
                    objectWindow.setX(stage.getX() + stage.getWidth());
                    objectWindow.setY(stage.getY());
                    objectWindow.show();
                    objectWindows.put(id, objectWindow);
                    objectWindow.setOnCloseRequest(e -> objectWindows.remove(id));
                }
            });
            return row;
        });
        tableNameColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        tableSizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        tableSizeColumn.setCellFactory(data -> new TableCell<S3ObjectSummary, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : NumberFormat.getNumberInstance().format(item));
            }
        });
        tableLastModifiedColumn.setCellValueFactory(new PropertyValueFactory<>("lastModified"));
        tableLastModifiedColumn.setCellFactory(data -> new TableCell<S3ObjectSummary, Date>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(item));
            }
        });

        // 活性条件をバインディング
        bucket.disableProperty().bind(progress.visibleProperty());
        refreshBucketsButton.disableProperty().bind(progress.visibleProperty());
        createBucketButton.disableProperty().bind(progress.visibleProperty());
        BooleanBinding bucketNotSelected = Bindings.isNull(bucket.valueProperty());
        deleteBucketButton.disableProperty().bind(bucketNotSelected.or(progress.visibleProperty()));
        uploadButton.disableProperty().bind(bucketNotSelected.or(progress.visibleProperty()));
        deleteButton.disableProperty().bind(bucketNotSelected.or(progress.visibleProperty())
                .or(objectList.getSelectionModel().selectedItemProperty().isNull()));

        // 一旦bucket窓閉じたら全部閉じるようにしとく
        stage.setOnCloseRequest(event ->
                objectWindows.values().stream().forEach(Stage::close));

        // Bucket一覧は最初に取得しとく
        listBucketsService.start();
    }

    private ListCell<Bucket> createBucketCell(ListView<Bucket> listView) {
        return new ListCell<Bucket>() {
            @Override
            protected void updateItem(Bucket item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getName());
            }
        };
    }

    private Stage createS3ObjectWindow(S3ObjectSummary item) {
        try {
            Stage stage = new Stage(StageStyle.DECORATED);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("s3object.fxml"));
            loader.setControllerFactory(clz -> new S3ObjectController(stage, client, item));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            return stage;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void searchObjects(KeyEvent event) {
        if (KeyCode.ENTER == event.getCode())
            listObjectsService.restart();
    }
}
