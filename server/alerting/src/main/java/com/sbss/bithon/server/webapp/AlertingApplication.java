package com.sbss.bithon.server.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author frankchen
 */
@ComponentScan("com.sbss.bithon")
@SpringBootApplication
public class AlertingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertingApplication.class, args);
    }
}
