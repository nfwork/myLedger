package com.myledger.api.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring MVC 与注入的 {@link com.fasterxml.jackson.databind.ObjectMapper} 统一使用
 * <strong>snake_case</strong> 序列化/反序列化 JavaBean 属性名（与前端、dbfound HTTP 返回 JSON 一致）。
 * Java 侧业务/DTO 字段为驼峰；dbfound 将行映射到 {@code Class} 时列名与 Bean 属性的对应由框架处理，与该项无关。
 */
@Configuration
public class JacksonJsonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonSnakeCaseNaming() {
        return builder -> builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }
}
