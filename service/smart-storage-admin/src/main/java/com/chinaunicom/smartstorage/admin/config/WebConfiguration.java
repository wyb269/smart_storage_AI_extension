package com.chinaunicom.smartstorage.admin.config;

import com.alibaba.fastjson.util.TypeUtils;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;
import com.google.common.collect.Lists;
import com.chinaunicom.smartstorage.common.converter.DateConverter;
import com.chinaunicom.smartstorage.common.desensitize.ValueDesensitizeFilter;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 资源配置类
 *
 * @author liuchink
 * @since 2018/6/19 19:48
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Bean
    public HttpMessageConverters fastJsonConverters() {
        FastJsonHttpMessageConverter fastJsonConverter = new FastJsonHttpMessageConverter();
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setWriterFeatures(JSONWriter.Feature.PrettyFormat, JSONWriter.Feature.WriteNullStringAsEmpty,
            JSONWriter.Feature.WriteNullListAsEmpty, JSONWriter.Feature.WriteMapNullValue,
            JSONWriter.Feature.WriteNulls);
        fastJsonConfig.setWriterFilters(new ValueDesensitizeFilter());
        List<MediaType> fastMediaTypes = Lists.newArrayList();
        fastMediaTypes.add(MediaType.APPLICATION_JSON);
        fastJsonConverter.setSupportedMediaTypes(fastMediaTypes);
        fastJsonConverter.setFastJsonConfig(fastJsonConfig);
        TypeUtils.compatibleWithJavaBean = true;
        TypeUtils.compatibleWithFieldName = true;
        return new HttpMessageConverters(fastJsonConverter);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new DateConverter());
    }

    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {
        corsRegistry.addMapping("/**").allowedOriginPatterns("*")
            .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS").allowCredentials(true).maxAge(3600)
            .allowedHeaders("*");
    }

}
