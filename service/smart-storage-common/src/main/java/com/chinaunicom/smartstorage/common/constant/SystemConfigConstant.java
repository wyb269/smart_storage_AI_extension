package com.chinaunicom.smartstorage.common.constant;

/**
 * 系统通用常量
 *
 * @author liuchink
 * @since 2018/10/28 13:04
 */
public final class SystemConfigConstant {
    /**
     * 初始化密码
     */
    public static final String ADMIN_RESET_USER_PWD_GROUP = "resetPwd";
    public static final String ADMIN_RESET_USER_PWD_KEY = "adminResetUserPwd";
    public static final String WECHAT_GROUP = "wechat";
    public static final String WECHAT_MINI_APPID_KEY = "miniAppId";
    public static final String WECHAT_MINI_APPSECRET_KEY = "miniAppSecret";
    public static final String MUST_UPDATE_PWD_DAY_GROUP = "mustUpdatePwdDay";
    public static final String MUST_UPDATE_PWD_DAY_ADMIN_KEY = "mustUpdatePwdDayAdmin";
    public static final String ADMIN_LOCK_PWD_GROUP = "lockPwd";
    public static final String ADMIN_LOCK_PWD_TIME_KEY = "adminLockPwdTime";
    public static final String APPLET_LOCK_PWD_GROUP = "lockPwd";
    public static final String APPLET_LOCK_PWD_TIME_KEY = "appletLockPwdTime";
    public static final String RSA_PUBLIC_KEY_GROUP = "rsaPublicKey";
    public static final String RSA_PUBLIC_KEY_KEY = "rsaPublicKey";
    public static final String RSA_PRIVATE_KEY_GROUP = "rsaPrivateKey";
    public static final String RSA_PRIVATE_KEY_KEY = "rsaPrivateKey";

    private SystemConfigConstant() {

    }
}
