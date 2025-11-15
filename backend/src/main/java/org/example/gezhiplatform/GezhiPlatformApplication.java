package org.example.gezhiplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class GezhiPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(GezhiPlatformApplication.class, args);
    }

}
