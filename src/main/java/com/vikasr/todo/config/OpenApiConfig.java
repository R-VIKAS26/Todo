package com.vikasr.todo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Todo API",
                version = "v1",
                description = "API for todo management, reminders, lists, analytics, import/export, and saved views.",
                contact = @Contact(name = "Todo API Support", email = "support@example.com"),
                license = @License(name = "Proprietary")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local")
        }
)
public class OpenApiConfig {

    @Bean
    public OpenAPI todoOpenAPI() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Todo API")
                        .version("v1")
                        .description("Production-style OpenAPI definition for the Todo backend."));
    }

    @Bean
    public GroupedOpenApi todoApiGroup() {
        return GroupedOpenApi.builder()
                .group("todo")
                .pathsToMatch("/api/v1/todos/**")
                .build();
    }
}
