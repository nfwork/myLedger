package com.myledger.api.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全局 JSON（Spring MVC 与注入的 {@link com.fasterxml.jackson.databind.ObjectMapper}）约定：
 * <ul>
 *   <li><strong>snake_case</strong> 属性名（与前端、dbfound HTTP 返回 JSON 一致）</li>
 *   <li><strong>NON_NULL</strong>：{@code null} 字段不输出，减少冗余键</li>
 * </ul>
 * DTO 上不使用 {@code @JsonProperty} / {@code @JsonInclude}，统一在此维护。
 */
@Configuration
public class JacksonJsonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonDefaults() {
        return builder -> builder
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .serializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
