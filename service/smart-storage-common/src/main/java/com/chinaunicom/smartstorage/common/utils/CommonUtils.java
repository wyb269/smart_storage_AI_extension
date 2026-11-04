package com.chinaunicom.smartstorage.common.utils;

import cn.hutool.extra.spring.SpringUtil;

/**
 * @author liugang
 * @since 2025/5/23 8:30
 */
public class CommonUtils {
    public static boolean checkActiveProfiles(String activeProfileName) {
        String[] activeProfiles = SpringUtil.getApplicationContext().getEnvironment().getActiveProfiles();
        for (String activeProfile : activeProfiles) {
            if (activeProfileName.equals(activeProfile)) {
                return true;
            }
        }
        return false;
    }
}
