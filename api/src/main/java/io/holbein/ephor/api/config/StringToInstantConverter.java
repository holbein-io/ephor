package io.holbein.ephor.api.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class StringToInstantConverter implements Converter<String, Instant> {
    @Override
    public Instant convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return Instant.parse(source);
    }
}
