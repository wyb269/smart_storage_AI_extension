package com.chinaunicom.smartstorage.applet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;

/**
 * @author liuchink
 * @since 2021/8/30 9:06
 */
@Configuration
public class MapperFactoryConfiguration {

    @Bean
    public MapperFactory getMapperFactory() {
        return new DefaultMapperFactory.Builder().build();
    }

    @Bean
    public MapperFacade getMapperFacade() {
        return new DefaultMapperFactory.Builder().build().getMapperFacade();
    }
}
