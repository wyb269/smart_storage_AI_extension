package com.chinaunicom.smartstorage.common.result;

import java.time.LocalDateTime;

import org.slf4j.MDC;

import com.alibaba.fastjson2.annotation.JSONField;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 状态结果
 *
 * @author liuchink
 * @since 2018/8/11 21:11
 */
@Data
public class StatusResult {

    public StatusResult() {
        this.code = SystemConstant.SUCCESS_STRING_200;
        this.message = "请求成功";
        this.requestId = MDC.get(SystemConstant.LOG_REQUEST_ID);
    }

    @Schema(description = "结果", nullable = true, example = "123456")
    @JSONField(name = "success")
    private boolean success;
    @Schema(description = "状态码", nullable = true, example = "123456")
    @JSONField(name = "code")
    private String code;
    @Schema(description = "返回信息", nullable = true, example = "123456")
    @JSONField(name = "message")
    private String message;
    @Schema(description = "请求ID", nullable = true, example = "123456")
    @JSONField(name = "requestId")
    private String requestId;
    @Schema(description = "应答时间", nullable = true, example = "123456")
    @JSONField(name = "timestamp")
    private LocalDateTime timestamp;
}
