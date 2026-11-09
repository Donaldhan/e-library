package com.elibrary.interfaces.http;

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
    public OpenAPI eLibraryOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Library Service API")
                        .version("1.0.0")
                        .description("Browse the catalogue, borrow and return digital titles, and join a "
                                + "waiting list when all licenses are in use. Demo tokens: token-alice, "
                                + "token-bob, token-carol."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .description("Opaque demo token, e.g. token-alice")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
