package org.hogedriven.s3fx;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Owner;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.hogedriven.s3fx.client.S3Adapter;
import org.hogedriven.s3fx.client.S3AdapterBuilder;

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
    public TextField proxy;
    public RadioButton defaultMode;
    public RadioButton mockMode;
    public RadioButton basicMode;
    public CheckBox readOnly;
    public TextField fixBucket;
    public CheckBox connectCheck;

    public S3ConfigController(Dialog<S3Adapter> dialog) {
        dialog.setResultConverter(this::createResult);
    }

    private S3Adapter createResult(ButtonType button) {
        if (button.getButtonData().isCancelButton()) return null;

        return new S3AdapterBuilder()
                .withProxy(proxy.getText())
                .readOnlyLock(readOnly.isSelected())
                .basicIf(basicMode.isSelected(), accessKey.getText(), secretKey.getText())
                .verifyIf(connectCheck.isSelected(), this::ownerCheck)
                .fixBucket(fixBucket.getText())
                .withMock(mockMode.isSelected())
                .build();
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

        loadProperty();
    }

    /**
     * プロパティファイルがあったら読む。
     */
    private void loadProperty() {
        File file = new File("s3fx.properties");
        if (!file.exists()) return;
        try (Reader reader = Files.newBufferedReader(file.toPath())) {
            Properties properties = new Properties();
            properties.load(reader);

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

            accessKey.setText(properties.getProperty("accessKey"));
            secretKey.setText(properties.getProperty("secretKey"));
            proxy.setText(properties.getProperty("proxy"));
            fixBucket.setText(properties.getProperty("fixBucket"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
