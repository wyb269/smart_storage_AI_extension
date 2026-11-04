package com.chinaunicom.smartstorage.common.optlog;

/**
 * 业务操作类型
 *
 * @author liuchink
 * @since 2022/7/6 14:12
 */
public enum OptLogBusinessType {
    /**
     * 后台用户
     */
    MANAGE("MANAGE"),

    /**
     * 手机端用户
     */
    APPLET("APPLET"),
    /**
     * 手机端用户
     */
    APP("APP");

    private final String value;

    OptLogBusinessType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
