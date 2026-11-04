package com.chinaunicom.smartstorage.admin.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import com.chinaunicom.smartstorage.admin.component.MyWordDenyComponent;

import jakarta.annotation.Resource;

/**
 * @author liuchink
 * @since 2024/9/30 14:48
 */
@Configuration
public class SensitiveWordBsConfiguration {
    @Resource
    private MyWordDenyComponent myWordDenyComponent;

    @Bean
    public SensitiveWordBs sensitiveWordBs() {
        return SensitiveWordBs.newInstance().wordDeny(WordDenys.chains(myWordDenyComponent)).ignoreCase(true)
            .ignoreWidth(true).ignoreNumStyle(true).ignoreChineseStyle(true).ignoreEnglishStyle(true).ignoreRepeat(true)
            .enableNumCheck(false).enableEmailCheck(false).enableUrlCheck(false).init();
    }
}
