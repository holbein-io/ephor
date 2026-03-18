package io.holbein.ephor.api.directory;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ephor.user-directory.github")
public class GitHubDirectoryProperties {

    private String org;
    private String token;
    private Map<String, String> teamMapping = Map.of();
    private String syncInterval = "30m";
}
