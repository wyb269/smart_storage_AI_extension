package com.chinaunicom.smartstorage.admin.constant;

/**
 * 系统通用常量
 *
 * @author liuchink
 * @since 2018/10/28 13:04
 */
public final class AdminSystemConstant {
    /**
     * 用户登录缓存Redis的key值
     */
    public static final String REDIS_MANAG_RIGHT_KEY = "user_right_details:";
    public static final String SESSION_VER_CODE = "admin_verify_code";
    public static final String VER_CODE_KEY = "admin_verify_code:";
    public static final int VER_CODE_KEY_TIME = 30;
    public static final String ORG_ROOT_CODE = "0";

    private AdminSystemConstant() {

    }
}
