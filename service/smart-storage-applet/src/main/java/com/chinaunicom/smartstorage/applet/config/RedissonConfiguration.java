package com.chinaunicom.smartstorage.applet.config;

import cn.hutool.crypto.SmUtil;
import com.alibaba.fastjson2.support.spring6.data.redis.FastJsonRedisSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RedissonConfig配置类
 *
 * @author liuchink
 * @since 2018/12/18 17:53
 */
@Slf4j
@Configuration
public class RedissonConfiguration {
    @Value("${spring.redis.redisson.file}")
    private String configFileLocation;

    @Bean
    public RedissonClient redissonClient() throws IOException {
        String configContent;
        String filePath;
        if (configFileLocation.startsWith("classpath:")) {
            filePath = configFileLocation.substring("classpath:".length());
            final ClassPathResource classPathResource = new ClassPathResource(filePath);
            try (InputStream inputStream = classPathResource.getInputStream()) {
                configContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } else if (configFileLocation.startsWith("file:")) {
            filePath = configFileLocation.substring("file:".length());
            configContent = Files.readString(new File(filePath).toPath(), StandardCharsets.UTF_8);
        } else {
            configContent = Files.readString(new File(configFileLocation).toPath(), StandardCharsets.UTF_8);
        }
        configContent = decryptPasswordInConfig(configContent);
        Config config = Config.fromYAML(configContent);
        return Redisson.create(config);
    }

    private String decryptPasswordInConfig(String configContent) {
        Matcher matcher = Pattern.compile("^\\s*password:\\s*(.+)$", Pattern.MULTILINE).matcher(configContent);
        String password = "";
        if (matcher.find()) {
            password = SmUtil.sm4(SystemConstant.DATASOURCE_SECRET_KEY.getBytes(StandardCharsets.UTF_8))
                .decryptStr(matcher.group(1).trim());
        }
        configContent = configContent.replaceAll("(?<=\\n|^)(\\s*password:\\s*)[^\\r\\n]*",
            Matcher.quoteReplacement("  password: " + password));
        return configContent;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        objectMapper.registerModule(new JavaTimeModule());
        PolymorphicTypeValidator ptv =
            BasicPolymorphicTypeValidator.builder().allowIfBaseType("com.chinaunicom.smartstorage")
                .allowIfBaseType("java.util").allowIfBaseType("java.time").build();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(serializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean(name = "fastJsonRedisTemplate")
    public RedisTemplate<String, Object> fastJsonRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new FastJsonRedisSerializer<>(Object.class));
        redisTemplate.setHashValueSerializer(new FastJsonRedisSerializer<>(Object.class));
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
