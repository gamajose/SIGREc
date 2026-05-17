package br.gov.sigrec.tfdapac;

import br.gov.sigrec.tfdapac.desktop.SigrecDesktopApplication;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TfdApacApplication {
    public static void main(String[] args) {
        if (desktopEnabled()) {
            Application.launch(SigrecDesktopApplication.class, args);
            return;
        }
        SpringApplication.run(TfdApacApplication.class, args);
    }

    public static SpringApplication app() {
        return new SpringApplication(TfdApacApplication.class);
    }

    private static boolean desktopEnabled() {
        String value = System.getenv().getOrDefault("SIGREC_DESKTOP", System.getProperty("sigrec.desktop", "true"));
        return Boolean.parseBoolean(value);
    }
}
