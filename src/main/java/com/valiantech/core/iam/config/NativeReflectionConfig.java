package com.valiantech.core.iam.config;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Configuration
@RegisterReflectionForBinding(JsonBinaryType.class)
public class NativeReflectionConfig {
}