package com.Omnibus.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration — provides API metadata, JWT security scheme,
 * tag ordering, and server definitions for Swagger UI and generated clients.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI OmnibusOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development"),
                        new Server().url("https://api.Omnibus.com").description("Production")
                ))
                .tags(List.of(
                        new Tag().name("Authentication").description("User registration and login — public endpoints"),
                        new Tag().name("Transfers").description("Money transfers with idempotency — requires JWT"),
                        new Tag().name("Accounts").description("Account queries — requires JWT")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Obtain a token via POST /api/v1/auth/login, then pass it as `Authorization: Bearer <token>`")
                        ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    private Info apiInfo() {
        return new Info()
                .title("Omnibus API")
                .version("1.0.0-SNAPSHOT")
                .description("""
                        Production-grade payment gateway with double-entry ledger, \
                        pessimistic locking, idempotency keys, and immutable audit trail.
                        
                        ## Key Guarantees
                        - **Exactly-once processing** via client-supplied idempotency keys (24h TTL)
                        - **Atomic double-entry** — every transfer creates balanced DEBIT + CREDIT entries
                        - **Deadlock-free** — accounts locked in deterministic UUID order
                        - **Audit trail** — every mutation logged with REQUIRES_NEW propagation
                        
                        ## Authentication
                        All endpoints except `/api/v1/auth/**` require a JWT bearer token.
                        Tokens expire after 15 minutes.
                        """);
    }
}
