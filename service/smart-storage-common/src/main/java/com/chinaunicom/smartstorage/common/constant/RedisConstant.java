package com.chinaunicom.smartstorage.common.constant;

/**
 * @author liuchink
 * @since 2024/06/20 08:53
 */
public class RedisConstant {
    /**
     * 系统配置
     */
    public static final String SYSTEM_CONFIG_KEY = "system_config_key";
    public static final String NO_REPEAT_ADMIN_KEY = "no_repeat_admin_key:";
    public static final String NO_REPEAT_APPLET_KEY = "no_repeat_applet_key:";
    // 作业-作业编号
    public static final String TASK_FLOW_INCR_KEY = "task_flow_incr_key";
    // 作业-登入
    public static final String TASK_FLOW_LOGIN_INCR_KEY = "task_flow_login_incr_key";
    // 作业-登出
    public static final String TASK_FLOW_LOGIN_OUT_INCR_KEY = "task_flow_login_out_incr_key";
    // 作业-扦样
    public static final String TASK_FLOW_SAMPLE_COLLECTION_INCR_KEY = "task_flow_sample_collection_incr_key";
    // 作业-质检
    public static final String TASK_FLOW_QUALITY_CHECK_INCR_KEY = "task_flow_quality_check_incr_key";
    // 作业-称重
    public static final String TASK_FLOW_WEIGHT_CHECK_INCR_KEY = "task_flow_weight_check_incr_key";
    // 作业-仓库
    public static final String TASK_FLOW_DATA_WAREHOUSE_INCR_KEY = "task_flow_data_warehouse_incr_key";
    // 作业-结算
    public static final String TASK_FLOW_SETTLEMENT_INCR_KEY = "task_flow_settlement_incr_key";
    // 作业-智能卡
    public static final String TASK_FLOW_SMART_CARD_INCR_KEY = "task_flow_smart_card_incr_key";
    public static final String TRANSFER_STORAGE_AUDIT_STATUS_KEY = "transfer_storage_audit_status_key:";

    private RedisConstant() {

    }
}
