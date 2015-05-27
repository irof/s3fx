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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import org.hogedriven.s3fx.client.S3Wrapper;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Supplier;

/**
 * @author irof
 */
public class S3BucketController implements Initializable {
    private final S3Wrapper client;
    public ProgressIndicator progress;
    private Optional<Bucket> currentBucket = Optional.empty();

    private final Stage stage;
    private final Map<S3ObjectIdentifier, Stage> objectWindows = new HashMap<>();

    public ComboBox<Bucket> bucket;
    private ObservableList<Bucket> buckets = FXCollections.observableArrayList();
    public Button deleteButton;
    public Button uploadButton;

    public ListView<S3ObjectSummary> objectList;
    public Button createBucketButton;
    public Button deleteBucketButton;

    private final Service<ObservableList<S3ObjectSummary>> listObjectsService;

    public S3BucketController(Stage stage, S3Wrapper client) {
        this.stage = stage;
        this.client = client;
        this.listObjectsService = createS3Service(() ->
                FXCollections.observableArrayList(client.listObjects(currentBucket.get())));
    }

    private <T> Service<T> createS3Service(Supplier<T> method) {
        return new Service<T>() {
            @Override
            protected Task<T> createTask() {
                return new Task<T>() {
                    @Override
                    protected T call() throws Exception {
                        return method.get();
                    }
                };
            }
        };
    }

    public void getBuckets() {
        buckets.clear();
        buckets.addAll(client.listBuckets());
    }

    public void createBucket() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("新しいBucketの作成");
        dialog.setHeaderText("作りたいBucketの名前を入力してください");
        dialog.setContentText("new Bucket Name :");

        dialog.showAndWait().ifPresent(name -> buckets.add(client.createBucket(name)));
    }

    public void deleteBucket() {
        currentBucket.ifPresent(client::deleteBucket);
        currentBucket.ifPresent(buckets::remove);
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
            client.putObject(currentBucket.get(), name, file);
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
        getBuckets();
        bucket.setItems(buckets);
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
            currentBucket = Optional.ofNullable(newValue);
            listObjectsService.restart();
        });

        objectList.itemsProperty().bind(listObjectsService.valueProperty());

        objectList.setCellFactory(this::createObjectCell);
        progress.visibleProperty().bind(listObjectsService.runningProperty());

        BooleanBinding bucketNotSelected = Bindings.isNull(bucket.valueProperty());
        deleteBucketButton.disableProperty().bind(bucketNotSelected);
        uploadButton.disableProperty().bind(bucketNotSelected);
        deleteButton.disableProperty().bind(bucketNotSelected
                .or(objectList.getSelectionModel().selectedItemProperty().isNull()));

        // 一旦bucket窓閉じたら全部閉じるようにしとく
        stage.setOnCloseRequest(event ->
                objectWindows.values().stream().forEach(Stage::close));
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

    private ListCell<S3ObjectSummary> createObjectCell(ListView<S3ObjectSummary> s3ObjectSummaryListView) {
        ListCell<S3ObjectSummary> listCell = new ListCell<S3ObjectSummary>() {
            @Override
            protected void updateItem(S3ObjectSummary item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty);
                setText(empty ? "" : item.getKey());
            }
        };
        listCell.setOnMouseClicked(event -> {
            S3ObjectSummary item = listCell.getItem();
            S3ObjectIdentifier id = new S3ObjectIdentifier(item);
            if (objectWindows.containsKey(id)) {
                objectWindows.get(id).requestFocus();
            } else if (event.getClickCount() == 2) {
                Stage objectWindow = createS3ObjectWindow(item);
                // 上を合わせて右に並べて出す
                objectWindow.setX(stage.getX() + stage.getWidth());
                objectWindow.setY(stage.getY());
                objectWindow.show();
                objectWindows.put(id, objectWindow);
                objectWindow.setOnCloseRequest(e -> objectWindows.remove(id));
            }
        });
        return listCell;
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
}
