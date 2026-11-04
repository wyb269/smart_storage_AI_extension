package com.chinaunicom.smartstorage.common.optlog;

import lombok.Getter;

/**
 *
 * 操作状态
 *
 * @author liuchink
 * @since 2022/7/6 14:12
 */

@Getter
public enum OptLogBusinessStatus {
    /**
     * 成功
     */
    SUCCESS("success"),

    /**
     * 失败
     */
    FAIL("fail");

    private final String value;

    OptLogBusinessStatus(String value) {
        this.value = value;
    }

}
