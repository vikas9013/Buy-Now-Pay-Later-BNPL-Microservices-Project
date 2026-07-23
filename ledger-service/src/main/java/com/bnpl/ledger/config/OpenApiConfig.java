package com.bnpl.ledger.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ledger Service API")
                        .version("1.0")
                        .description("BNPL Transaction, Repayment & OAuth 2.0 / JWT Resource Server Service"))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuthentication"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuthentication",
                                new SecurityScheme()
                                        .name("BearerAuthentication")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
