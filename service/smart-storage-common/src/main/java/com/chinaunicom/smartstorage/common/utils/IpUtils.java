package com.chinaunicom.smartstorage.common.utils;


import jakarta.servlet.http.HttpServletRequest;

/**
 * ip工具类
 *
 * @author liuchink
 * @since 2019/4/9 9:26
 */
public class IpUtils {

	/**
	 * 获取IP地址
	 *
	 * @param request
	 *            request
	 * @return java.lang.String
	 * @author liuchink
	 * @since 2019/4/9 9:26
	 */
	public static String getRemoteHost(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
	}
}
