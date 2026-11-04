package com.chinaunicom.smartstorage.common.utils;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * cookieUtils
 *
 * @author liuchink
 * @since 2019/10/22 20:38
 */
public class CookieUtils {

	/**
	 * 获取cookie信息
	 *
	 * @param cookieName
	 *            cookieName
	 * @param request
	 *            request
	 * @return java.lang.String
	 * @author liuchink
	 */
	public static String getCookie(HttpServletRequest request,
								   String cookieName) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(cookieName)) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	/**
	 * 添加cookie信息
	 *
	 * @param cookieName
	 *            cookieName
	 * @param response
	 *            response
	 * @param value
	 *            value
	 * @author liuchink
	 */
	public static void writeCookie(HttpServletResponse response,
								   String cookieName, String value) {
		Cookie cookie = new Cookie(cookieName, value);
		cookie.setPath("/");
		cookie.setMaxAge(3600 * 24 * 30);
		response.addCookie(cookie);
	}

	/**
	 * 添加cookie信息
	 *
	 * @param cookieName
	 *            cookieName
	 * @param response
	 *            response
	 * @param value
	 *            value
	 * @author liuchink
	 */
	public static void writeCookie(HttpServletResponse response,
			String cookieName, String value, int maxAge) {
		Cookie cookie = new Cookie(cookieName, value);
		cookie.setPath("/");
		cookie.setMaxAge(maxAge);
		response.addCookie(cookie);
	}
}
