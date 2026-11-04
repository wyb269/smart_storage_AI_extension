package com.chinaunicom.smartstorage.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 *
 *
 * @author liuchink
 * @since 2018/8/29 14:27
 */
@Data
@Configuration
public class ChinkFrameConfiguration {
    @Value(("${chinkframe.application.projectVersion}"))
    private String projectVersion;
    @Value(("${chinkframe.application.projectName}"))
    private String projectName;
    @Value(("${chinkframe.application.projectDisplay}"))
    private String projectDisplay;
    @Value(("${chinkframe.application.projectContextPath}"))
    private String projectContextPath;

}
