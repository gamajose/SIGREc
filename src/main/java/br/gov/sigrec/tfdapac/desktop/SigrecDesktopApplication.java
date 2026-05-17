package br.gov.sigrec.tfdapac.desktop;

import br.gov.sigrec.tfdapac.TfdApacApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SigrecDesktopApplication extends Application {
    private ConfigurableApplicationContext context;
    private final int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        Label status = new Label("Iniciando SIGREc...");
        ProgressIndicator progress = new ProgressIndicator();

        VBox loading = new VBox(12, progress, status);
        loading.setPadding(new Insets(24));
        loading.setStyle("-fx-alignment: center; -fx-background-color: #f6f8fa;");

        StackPane center = new StackPane(loading, webView);
        webView.setVisible(false);

        BorderPane root = new BorderPane(center);
        Scene scene = new Scene(root, 1280, 820);
        stage.setTitle("SIGREc - TFD/APAC");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();

        startBackend();
        waitAndLoad(webView, loading, status);
    }

    private void startBackend() {
        Thread thread = new Thread(() -> {
            System.setProperty("sigrec.desktop", "false");
            System.setProperty("SIGREC_OPEN_BROWSER", "false");
            context = TfdApacApplication.app().run();
        }, "sigrec-backend");
        thread.setDaemon(true);
        thread.start();
    }

    private void waitAndLoad(WebView webView, VBox loading, Label status) {
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(() -> {
            if (!serverReady()) {
                Platform.runLater(() -> status.setText("Carregando banco de dados e modulos..."));
                return;
            }
            executor.shutdown();
            Platform.runLater(() -> {
                webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        loading.setVisible(false);
                        webView.setVisible(true);
                    }
                });
                webView.getEngine().load("http://localhost:" + port);
            });
        }, 0, 700, TimeUnit.MILLISECONDS);
    }

    private boolean serverReady() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/login").openConnection();
            connection.setConnectTimeout(250);
            connection.setReadTimeout(250);
            return connection.getResponseCode() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }
}
