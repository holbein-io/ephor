package io.holbein.ephor.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class IngestRetryConfig {

    @Bean
    public RetryTemplate ingestRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(200, 2.0, 2000, true)
                .retryOn(CannotAcquireLockException.class)
                .build();
    }
}
