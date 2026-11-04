package com.chinaunicom.smartstorage.applet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * @author liuchink
 * @since 2023/2/13 17:18
 */
@Data
@Configuration
public class PathConfiguration {
    @Value("${chinkframe.upload.context.path}")
    private String uploadContextPath;
    @Value("${chinkframe.upload.path.disk}")
    private String uploadPathDisk;
}
