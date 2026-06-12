package com.travelmind;

import com.travelmind.config.DotenvInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TravelMind 应用入口
 */
@SpringBootApplication
public class TravelMindApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TravelMindApplication.class);
        app.addInitializers(new DotenvInitializer());
        app.run(args);
    }
}
