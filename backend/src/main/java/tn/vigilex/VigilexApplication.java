/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VigilexApplication {
    public static void main(String[] args) {
        SpringApplication.run(VigilexApplication.class, args);
    }
}
