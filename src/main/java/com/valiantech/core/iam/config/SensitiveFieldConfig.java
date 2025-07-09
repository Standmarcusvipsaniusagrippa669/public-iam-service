package com.valiantech.core.iam.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Component
public class SensitiveFieldConfig {

    private final Set<String> sensitiveFields;

    public SensitiveFieldConfig(@Value("${logging.sensitive-fields:password}") String fields) {
        this.sensitiveFields = Arrays.stream(fields.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

}
