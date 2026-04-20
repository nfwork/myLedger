package com.myledger.api.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring MVC 与注入的 {@link com.fasterxml.jackson.databind.ObjectMapper} 统一使用
 * <strong>snake_case</strong> 序列化/反序列化 JavaBean 属性名（与前端、dbfound 查询 JSON 一致）。
 */
@Configuration
public class JacksonJsonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonSnakeCaseNaming() {
        return builder -> builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }
}
