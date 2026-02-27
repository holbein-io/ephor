package io.holbein.ephor.api.config;

import io.holbein.ephor.api.model.enums.SeverityLevel;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToSeverityLevelConverter implements Converter<String, SeverityLevel> {
    @Override
    public SeverityLevel convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return SeverityLevel.valueOf(source.toUpperCase());
    }
}