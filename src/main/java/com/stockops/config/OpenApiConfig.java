package com.stockops.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata configuration for the StockOps backend.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "StockOps API",
        version = "v1",
        description = "Smart inventory management API for small stores.",
        contact = @Contact(name = "StockOps Team"),
        license = @License(name = "Proprietary")
))
public class OpenApiConfig {
}
