package com.chinaunicom.smartstorage.common.jwt;

/**
 * 系统通用常量
 *
 * @author liuchink
 * @since 2018/10/28 13:04
 */
public final class JwtConstant {
    /**
     * Token过期时间必须大于生效时间
     */
    public static final int TOKEN_EXPIRE_TIME = (24 * 60 * 60) * 30;
    public static final int TOKEN_REFRESH_EXPIRE_TIME = (24 * 60 * 60) * 30;
    /**
     * token头标识
     */
    public static final String REQUEST_HEAD_TOKEN = "Authorization";
    public static final String JWT_SEPARATOR = "Bearer ";
    /**
     * admin
     */
    public static final String JWT_ISS_ADMIN = "SMART-STORAGE-ADMIN";
    public static final String JWT_AUD_ADMIN = "SMART-STORAGE-ADMIN";
    public static final String JWT_ADMIN_SECRET = "SXB61AcjvaBqBdilPHbSv30YQ39Nx1XHkJlYO8jhJ281Uhr61VIrdtwe2rba3oD8";
    public static final String JWT_ADMIN_SECRET_REFRESH =
        "QODXj1ZjieJFYLIZa3n8JZPSXB6fgkX1cPu7qMTQhK0GFWpjmA0xagavEgIkaWlr";
    /**
     * applet
     */
    public static final String JWT_ISS_APPLET = "SMART-STORAGE-APPLET";
    public static final String JWT_AUD_APPLET = "SMART-STORAGE-APPLET";
    public static final String JWT_APPLET_SECRET = "yVdUzYDSDC9xf3XvumZVq4fJOARHvsxSsQlNVaZVdDu7hgqwQVYs5ldbo4YX68wb";
    public static final String JWT_APPLET_SECRET_REFRESH =
        "yiwMa1g3avwu4xg4odpRlqfOLxQzwPJOsYOWNHzr9BnY4neFVdaAWx2zLduvDzgX";
    /**
     * app
     */
    public static final String JWT_ISS_APP = "SMART-STORAGE-APP";
    public static final String JWT_AUD_APP = "SMART-STORAGE-APP";
    public static final String JWT_APP_SECRET = "0WzclCvJJfqjDeimisP1leEuEsXwKKQdsnYGRkkZZTcA68wK6uPiMMy46UbxhmBf";
    public static final String JWT_APP_SECRET_REFRESH =
        "AX3HbksbtC2TFziMwXFpN6gdVp7OElp7A6DOjE68v54x3X7po1B73jMXpaTK1NAT";

    private JwtConstant() {

    }
}
