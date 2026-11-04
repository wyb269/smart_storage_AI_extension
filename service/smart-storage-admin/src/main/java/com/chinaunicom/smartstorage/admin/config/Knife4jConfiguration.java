package com.chinaunicom.smartstorage.admin.config;

import com.github.xingfudeshi.knife4j.spring.annotations.EnableKnife4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import com.chinaunicom.smartstorage.common.config.ChinkFrameConfiguration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.annotation.Resource;

/**
 *
 * @author liuchink
 * @since 2021/5/7 15:56
 */
@Configuration
@EnableKnife4j
public class Knife4jConfiguration {
    @Resource
    private ChinkFrameConfiguration chinkFrameConfiguration;
    @Bean
    public OpenAPI customOpenApi() {
        Contact contact = new Contact();
        contact.setName("liuchink");
        contact.setEmail("liuchink@163.com");
        return new OpenAPI()
                .info(new Info().title(chinkFrameConfiguration.getProjectName())
                        .version(chinkFrameConfiguration.getProjectVersion())
                        .description(chinkFrameConfiguration.getProjectName()).contact(contact))
                .addSecurityItem(new SecurityRequirement().addList(HttpHeaders.AUTHORIZATION))
                .components(new Components().addSecuritySchemes(HttpHeaders.AUTHORIZATION,
                        new SecurityScheme().name(HttpHeaders.AUTHORIZATION).type(SecurityScheme.Type.HTTP).scheme("bearer")));
    }

    @Bean
    public GroupedOpenApi groupedOpenApi() {
        String[] paths = {"/**"};
        String[] packagedToMatch = {"com.chinaunicom.smartstorage.admin"};
        return GroupedOpenApi.builder().group(chinkFrameConfiguration.getProjectName()).pathsToMatch(paths)
                .packagesToScan(packagedToMatch).build();
    }
}
