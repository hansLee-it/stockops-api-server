package com.stockops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * StockOps backend application entry point.
 *
 * @author StockOps Team
 * @since 1.0
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class StockOpsApplication {

    /**
     * Starts the StockOps Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(StockOpsApplication.class, args);
    }
}
