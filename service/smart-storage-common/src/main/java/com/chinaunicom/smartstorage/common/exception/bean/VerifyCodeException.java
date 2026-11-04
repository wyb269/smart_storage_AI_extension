package com.chinaunicom.smartstorage.common.exception.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 验证码异常
 *
 * @author liuchink
 * @since 2020/9/11 14:05
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class VerifyCodeException extends CustomException {


	public VerifyCodeException() {
		super(CustomExceptionCode.VERIFY_CODE_EXCEPTION_CODE,
				CustomExceptionCode.VERIFY_CODE_EXCEPTION_CODE_MESSAGE);
	}

}
