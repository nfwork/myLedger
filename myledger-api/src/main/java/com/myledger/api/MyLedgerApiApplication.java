package com.myledger.api;

import com.myledger.api.config.JwtSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class MyLedgerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyLedgerApiApplication.class, args);
    }
}
