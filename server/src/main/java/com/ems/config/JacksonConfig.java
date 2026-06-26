package com.ems.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局配置:
 * 将 Long/long 序列化为 String,避免前端 JS Number 精度丢失
 * (雪花算法生成的 19 位 Long 超过 JS 安全整数范围 2^53)
 * 注意:用 serializerByType 逐类型注册,避免 builder.modules() 覆盖默认 JavaTimeModule
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonLongToStringCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
