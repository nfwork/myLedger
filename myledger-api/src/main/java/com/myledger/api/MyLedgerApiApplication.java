package com.myledger.api;

import com.myledger.api.config.JwtSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class MyLedgerApiApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(MyLedgerApiApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MyLedgerApiApplication.class);
    }
}
