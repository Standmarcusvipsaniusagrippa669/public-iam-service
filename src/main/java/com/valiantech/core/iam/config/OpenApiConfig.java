package com.valiantech.core.iam.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI iamOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Valiantech IAM API")
                        .description("API para gestión de identidad y acceso")
                        .version("v1.0.0")
                        .license(new License().name("Licencia Interna").url("https://valiantech.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentación adicional")
                        .url("https://valiantech.com/docs"));
    }
}
