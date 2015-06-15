package s3fx;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import s3fx.client.S3Adapter;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * @author irof
 */
public class ControlTower implements Initializable {
    private final Stage stage;
    private final S3Adapter client;

    private static final Set<ObjectLister> windows = new HashSet<>();

    public ProgressIndicator indicator;

    public ControlTower(Stage stage, S3Adapter client) {
        this.stage = stage;
        this.client = client;
        stage.setOnCloseRequest(event -> Platform.exit());
    }

    public void refresh() {
        windows.stream().forEach(ObjectLister::refresh);
    }

    public void add() throws IOException {
        ObjectLister controller = new ObjectLister(stage, client);
        Stage stage = new Stage(StageStyle.DECORATED);
        FXMLLoader loader = new FXMLLoader(ObjectLister.getFXML());
        loader.setControllerFactory(clz -> controller);
        stage.setScene(new Scene(loader.load()));
        windows.add(controller);
        stage.setOnCloseRequest(handler -> windows.remove(controller));
        stage.show();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }
}
