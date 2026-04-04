package io.holbein.ephor.api.config;

import io.holbein.ephor.api.filter.GzipDecompressionFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SbomFilterConfig {

    @Bean
    public FilterRegistrationBean<GzipDecompressionFilter> gzipDecompressionFilter() {
        FilterRegistrationBean<GzipDecompressionFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new GzipDecompressionFilter());
        registration.addUrlPatterns("/sbom/ingest");
        registration.setOrder(1);
        return registration;
    }
}
