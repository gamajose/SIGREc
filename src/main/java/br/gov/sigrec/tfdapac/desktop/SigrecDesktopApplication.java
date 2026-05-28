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
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.springframework.context.ConfigurableApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import java.awt.Desktop;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SigrecDesktopApplication extends Application {
    private static final Pattern IMPRESSAO_URL_PATTERN = Pattern.compile("(^|.*/)?impressao/(\\d+)(?:/.*)?(?:[?#].*)?$");

    private ConfigurableApplicationContext context;
    private DesktopBridge desktopBridge;
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
                WebEngine engine = webView.getEngine();

                engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        loading.setVisible(false);
                        webView.setVisible(true);
                        configurarPonteJavascript(engine);
                        interceptarLinksPdf(engine);
                    }
                });

                engine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
                    Optional<String> urlPdf = normalizarUrlPdf(newUrl);
                    if (urlPdf.isPresent()) {
                        abrirNoNavegadorExterno(urlPdf.get());
                        Platform.runLater(() -> {
                            if (oldUrl != null && !normalizarUrlPdf(oldUrl).isPresent()) {
                                engine.load(oldUrl);
                            } else {
                                engine.load("http://localhost:" + port);
                            }
                        });
                    }
                });

                engine.load("http://localhost:" + port);
            });
        }, 0, 700, TimeUnit.MILLISECONDS);
    }

    private void configurarPonteJavascript(WebEngine engine) {
        try {
            desktopBridge = new DesktopBridge();
            JSObject window = (JSObject) engine.executeScript("window");
            window.setMember("sigrecDesktop", desktopBridge);
            engine.executeScript("""
                    (function() {
                        if (window.__sigrecPdfInterceptorInstalled) {
                            return;
                        }
                        window.__sigrecPdfInterceptorInstalled = true;
                        document.addEventListener('click', function(event) {
                            var element = event.target;
                            while (element && element.tagName !== 'A') {
                                element = element.parentElement;
                            }
                            if (!element) {
                                return;
                            }
                            var href = element.getAttribute('href') || element.href;
                            if (!href || !/(^|\\/)impressao\\/\\d+(?:\\/|$|[?#])/.test(href)) {
                                return;
                            }
                            event.preventDefault();
                            event.stopPropagation();
                            event.stopImmediatePropagation();
                            window.sigrecDesktop.openPdf(href);
                        }, true);
                    })();
                    """);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void interceptarLinksPdf(WebEngine engine) {
        Document document = engine.getDocument();
        if (document == null) {
            return;
        }

        NodeList links = document.getElementsByTagName("a");
        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            String href = link.getAttribute("href");
            Optional<String> urlPdf = normalizarUrlPdf(href);
            if (urlPdf.isEmpty()) {
                continue;
            }

            ((EventTarget) link).addEventListener("click", new EventListener() {
                @Override
                public void handleEvent(Event event) {
                    event.preventDefault();
                    event.stopPropagation();
                    abrirNoNavegadorExterno(urlPdf.get());
                }
            }, true);
        }
    }

    private Optional<String> normalizarUrlPdf(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }

        String urlTratada = url.trim();
        Matcher matcher = IMPRESSAO_URL_PATTERN.matcher(urlTratada);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.of("http://localhost:" + port + "/impressao/" + matcher.group(2));
    }

    private void abrirNoNavegadorExterno(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }

            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", url).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final class DesktopBridge {
        public void openPdf(String url) {
            normalizarUrlPdf(url).ifPresent(SigrecDesktopApplication.this::abrirNoNavegadorExterno);
        }
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
