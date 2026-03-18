package io.holbein.ephor.api.directory;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ephor.user-directory.keycloak")
public class KeycloakDirectoryProperties {

    private String serverUrl;
    private String realm = "ephor";
    private String clientId = "ephor-admin";
    private String clientSecret;
    private String syncInterval = "15m";
}
