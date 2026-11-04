package com.chinaunicom.smartstorage.admin.security;

import org.springframework.security.crypto.password.PasswordEncoder;

import cn.hutool.core.convert.Convert;
import cn.hutool.crypto.SecureUtil;

/**
 * 密码加密
 *
 * @author liuchink
 * @since 2020/6/11 15:19
 */
public class UserLoginPasswordEncoder implements PasswordEncoder {
	@Override
	public String encode(CharSequence charSequence) {
		return SecureUtil.md5(Convert.toStr(charSequence));
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		return encodedPassword
				.equals(SecureUtil.md5(Convert.toStr(rawPassword)));
	}
}
