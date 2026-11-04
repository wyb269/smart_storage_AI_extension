package com.chinaunicom.smartstorage.common.constant;

/**
 * 系统通用常量
 *
 * @author liuchink
 * @since 2018/10/28 13:04
 */
public final class SystemConstant {
    /**
     * 框架版本号,作为框架版本识别，请勿更改
     */
    public static final String FRAME_VERSION = "v0.0.1";
    /**
     * 成功标记-字符串
     */
    public static final String SUCCESS_STRING_SUCCESS = "SUCCESS";
    /**
     * 成功标记-字符串-0
     */
    public static final String SUCCESS_STRING_0 = "0";
    /**
     * 成功标记-字符串-0
     */
    public static final String SUCCESS_STRING_200 = "200";
    /**
     * 成功标记-字符串-ok
     */
    public static final String SUCCESS_STRING_OK = "OK";
    /**
     * 成功标记-整形
     */
    public static final int SUCCESS_INT_0 = 0;
    /**
     * 成功标记-整形
     */
    public static final int SUCCESS_INT_200 = 200;
    /**
     * 系统异常标题
     */
    public static final String LOG_ERROR_TITLE = "系统异常信息:{}";
    public static final String LOG_INFO_TITLE = "[{}]:{}";
    public static final String LOG_DEBUG_TITLE = "[{}]:{}";
    public static final String ADMIN_LOGIN_ACCESS_TOKEN_KEY = "admin_login_access_token:";
    public static final String ADMIN_LOGIN_REFRESH_TOKEN_KEY = "admin_login_refresh_token:";
    public static final String APPLET_LOGIN_ACCESS_TOKEN_KEY = "applet_login_access_token:";
    public static final String APPLET_LOGIN_REFRESH_TOKEN_KEY = "applet_login_refresh_token:";
    public static final String APP_LOGIN_ACCESS_TOKEN_KEY = "app_login_access_token:";
    public static final String APP_LOGIN_REFRESH_TOKEN_KEY = "app_login_refresh_token:";
    public static final String LOG_REQUEST_ID = "requestId";
    public static final String DATASOURCE_SECRET_KEY = System.getenv().getOrDefault("SMART_STORAGE_DATASOURCE_SECRET_KEY", "");
    public static final String ACTIVE_PROFILE_DEV = "dev";
    public static final String ACTIVE_PROFILE_TES = "site";
    public static final String ACTIVE_PROFILE_PRO = "uat";
    public static final String MD5_SOLT = System.getenv().getOrDefault("SMART_STORAGE_MD5_SALT", "");
    private SystemConstant() {

    }
}
