package com.chinaunicom.smartstorage.common.exception.bean;

/**
 * 表单提交token异常
 *
 * @author liuchink
 * @since 2019/5/14 22:20
 */
public class SubmitTokenException extends CustomException {


	public SubmitTokenException() {
		super(CustomExceptionCode.SUBMIT_SAME_EXCEPTION_CODE,
				CustomExceptionCode.SUBMIT_SAME_EXCEPTION_CODE_MESSAGE);
	}
}
