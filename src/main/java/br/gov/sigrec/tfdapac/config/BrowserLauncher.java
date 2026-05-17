package br.gov.sigrec.tfdapac.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;
import java.util.Locale;

@Component
public class BrowserLauncher {
    private final boolean openBrowser;
    private final int port;

    public BrowserLauncher(@Value("${sigrec.open-browser:true}") boolean openBrowser,
                           @Value("${server.port:8080}") int port) {
        this.openBrowser = openBrowser;
        this.port = port;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void open() {
        if (!openBrowser) {
            return;
        }
        String url = "http://localhost:" + port;
        try {
            openWithOperatingSystem(url);
        } catch (Exception ignored) {
            // In server/headless environments the URL can still be opened manually.
        }
    }

    private void openWithOperatingSystem(String url) throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            new ProcessBuilder("cmd", "/c", "start", "", url).start();
            return;
        }
        if (os.contains("mac")) {
            new ProcessBuilder("open", url).start();
            return;
        }
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI(url));
            return;
        }
        new ProcessBuilder("xdg-open", url).start();
    }
}
