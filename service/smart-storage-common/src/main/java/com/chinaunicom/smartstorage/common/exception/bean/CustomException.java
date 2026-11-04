package com.chinaunicom.smartstorage.common.exception.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 自定义异常信息
 *
 * @author liuchink
 * @since 2018/8/23 11:25
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class CustomException extends RuntimeException {

    private final String errorCode;
    private final String errorMsg;

    public CustomException(String errorCode, String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public CustomException() {
        super(CustomExceptionCode.DEFAULT_EXCEPTION_MESSAGE);
        this.errorCode = CustomExceptionCode.DEFAULT_EXCEPTION_CODE;
        this.errorMsg = CustomExceptionCode.DEFAULT_EXCEPTION_MESSAGE;
    }

    public CustomException(String errorMsg) {
        super(errorMsg);
        this.errorCode = CustomExceptionCode.DEFAULT_EXCEPTION_CODE;
        this.errorMsg = errorMsg;
    }

    public CustomException(Throwable throwable) {
        super(throwable);
        if (throwable instanceof CustomException customException) {
            this.errorCode = customException.getErrorCode();
            this.errorMsg = customException.getErrorMsg();
        } else {
            this.errorCode = CustomExceptionCode.DEFAULT_EXCEPTION_CODE;
            this.errorMsg = throwable.getMessage();
        }
    }
}
