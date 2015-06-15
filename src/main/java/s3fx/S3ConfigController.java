package s3fx;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Owner;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import s3fx.client.S3AdapterBuilder;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * @author irof
 */
public class S3ConfigController implements Initializable {

    public TextField accessKey;
    public PasswordField secretKey;

    public ToggleGroup modeGroup;
    public RadioButton defaultMode;
    public RadioButton mockMode;
    public RadioButton basicMode;

    public TextField proxy;
    public CheckBox readOnly;
    public TextField fixBucket;
    public CheckBox connectCheck;

    public ToggleGroup clientType;
    public RadioButton normalClient;
    public RadioButton bucketsClient;

    private S3AdapterBuilder builder;

    public S3ConfigController(Dialog<S3fxConfig> dialog) {
        dialog.setResultConverter(this::createResult);
    }

    private S3fxConfig createResult(ButtonType button) {
        if (button.getButtonData().isCancelButton()) return null;

        Class<?> clientClass = (Class<?>) clientType.getSelectedToggle().getUserData();
        return new S3fxConfig(
                builder.verifyIf(connectCheck.isSelected(), this::ownerCheck),
                clientClass);
    }

    private void ownerCheck(AmazonS3 client) {
        Owner owner = client.getS3AccountOwner();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("S3に接続できました");
        alert.setContentText("Account Owner Name: " + owner.getDisplayName());
        alert.showAndWait();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        accessKey.disableProperty().bind(basicMode.selectedProperty().not());
        secretKey.disableProperty().bind(basicMode.selectedProperty().not());
        proxy.disableProperty().bind(mockMode.selectedProperty());

        bindBuilder();
    }

    private void bindBuilder() {
        Properties properties = new Properties();
        File file = new File("s3fx.properties");
        if (file.exists()) {
            try (Reader reader = Files.newBufferedReader(file.toPath())) {
                properties.load(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        builder = new S3AdapterBuilder(properties);

        accessKey.textProperty().bindBidirectional(builder.accessKey);
        secretKey.textProperty().bindBidirectional(builder.secretKey);
        proxy.textProperty().bindBidirectional(builder.proxy);
        fixBucket.textProperty().bindBidirectional(builder.bucket);
        readOnly.selectedProperty().bindBidirectional(builder.readOnly);

        basicMode.selectedProperty().bindBidirectional(builder.basicMode);
        mockMode.selectedProperty().bindBidirectional(builder.mockMode);

        String mode = properties.getProperty("mode", "default");
        switch (mode) {
            case "mock":
                modeGroup.selectToggle(mockMode);
                break;
            case "basic":
                modeGroup.selectToggle(basicMode);
                break;
            default:
                modeGroup.selectToggle(defaultMode);
        }

        normalClient.setUserData(S3BucketController.class);
        bucketsClient.setUserData(ControlTower.class);
    }
}
