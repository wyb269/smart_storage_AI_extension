package com.chinaunicom.smartstorage.common.optlog;

/**
 * 业务操作类型
 *
 * @author liuchink
 * @since 2022/7/6 14:12
 */
public enum OptLogStorageType {
    /**
     * DB
     */
    DB("DB"),

    /**
     * RABBITMQ
     */
    RABBITMQ("RABBITMQ");

    private final String value;

    OptLogStorageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
