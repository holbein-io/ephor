package io.holbein.ephor.api.config;

import io.holbein.ephor.api.entity.VulnerabilityInstance.InstanceStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToInstanceStatusConverter implements Converter<String, InstanceStatus> {
    @Override
    public InstanceStatus convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return InstanceStatus.valueOf(source.toLowerCase());
    }
}