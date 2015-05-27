package org.hogedriven.s3fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.hogedriven.s3fx.client.S3Adapter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

/**
 * @author irof
 */
public class Starter extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        S3Adapter client = createAmazonS3Client();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("s3client.fxml"));
        loader.setControllerFactory(clz -> new S3BucketController(stage, client));
        stage.setTitle("S3FX - JavaFX Amazon S3 Client");
        stage.setScene(new Scene(loader.load()));

        setExceptionHandler();
        stage.show();
    }

    private S3Adapter createAmazonS3Client() throws IOException {
        Dialog<S3Adapter> dialog = new Dialog<>();

        FXMLLoader loader = new FXMLLoader(this.getClass().getResource("s3config.fxml"));
        loader.setControllerFactory(clz -> new S3ConfigController(dialog));
        dialog.getDialogPane().setContent(loader.load());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        return dialog.showAndWait().orElseThrow(() -> new RuntimeException("キャンセルしたので起動しません"));
    }

    private void setExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            // 標準出力にも吐いておく
            e.printStackTrace();

            // ダイアログだしとく
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("こまりました！");
            alert.setHeaderText("なんか例外だよ");
            alert.setContentText("気になる人のためのスタックトレース");
            try (StringWriter stringWriter = new StringWriter();
                 PrintWriter writer = new PrintWriter(stringWriter)) {
                if (e instanceof RuntimeException
                        && e.getCause() instanceof InvocationTargetException) {
                    Throwable ite = e.getCause();
                    Throwable cause = ite.getCause();
                    cause.printStackTrace(writer);
                } else {
                    e.printStackTrace(writer);
                }
                alert.getDialogPane().setExpandableContent(new TextArea(stringWriter.toString()));
            } catch (IOException e1) {
                // きにしない
                e1.printStackTrace();
            }
            alert.show();
        });
    }
}
