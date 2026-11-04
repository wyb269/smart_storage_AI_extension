package com.chinaunicom.smartstorage.admin.enumbean;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskStatusEnum {
    /**
     * 已登入
     */
    LOGGED_IN("logIn", "已登入"),

    /**
     * 已采样
     */
    SAMPLED("sampled", "已仟样"),

    /**
     * 已质检
     */
    INSPECTED("inspected", "已质检"),

    /**
     * 已称重
     */
    WEIGHED("weighed", "已称重"),

    /**
     * 已入仓
     */
    WAREHOUSED("warehoused", "已值仓"),

    /**
     * 已称皮重
     */
    TARE_WEIGHED("tareWeighed", "已称皮重"),

    /**
     * 已结算
     */
    SETTLED("settled", "已结算"),

    /**
     * 已登出
     */
    LOGOUT("logout", "已登出"),

    /**
     * 待销卡
     */
    PEND_CARD("pendCard", "待销卡");

    /**
     * 编码
     */
    private final String code;

    /**
     * 描述
     */
    private final String description;

    public static TaskStatusEnum fromCode(String code) {
        for (TaskStatusEnum taskStatusEnum : TaskStatusEnum.values()) {
            if (taskStatusEnum.code.equals(code)) {
                return taskStatusEnum;
            }
        }
        throw new IllegalArgumentException("未找到匹配的TaskStatusEnum，code: " + code);
    }
}
