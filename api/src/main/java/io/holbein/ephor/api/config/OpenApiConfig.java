package io.holbein.ephor.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ephor API")
                        .description("Vulnerability management platform for Kubernetes environments")
                        .version("0.0.1-SNAPSHOT")
                        .license(new License()
                                .name("AGPL-3.0-only")
                                .url("https://www.gnu.org/licenses/agpl-3.0.html"))
                        .contact(new Contact()
                                .name("holbein-io")
                                .url("https://github.com/holbein-io/ephor")));
    }
}
