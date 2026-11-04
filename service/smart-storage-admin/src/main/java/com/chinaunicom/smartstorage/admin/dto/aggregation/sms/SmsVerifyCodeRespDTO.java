package com.chinaunicom.smartstorage.admin.dto.aggregation.sms;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 手机验证码
 *
 * @author liuchink
 * @since 2022/1/30 9:17
 */
@Data
public class SmsVerifyCodeRespDTO  {

    /**
     * 手机号码
     */
    @Schema(description = "手机号码", nullable = true, example = "12345")
    private String phone;

    /**
     * 短信内容
     */
    @Schema(description = "短信内容", nullable = true, example = "12345")
    private String details;

    /**
     * 验证码
     */
    @Schema(description = "验证码", nullable = true, example = "12345")
    private String code;

    /**
     * 过期时间
     */
    @Schema(description = "过期时间", nullable = true, example = "12345")
    private LocalDateTime expiredTime;
    /**
     * 状态
     */
    @Schema(description = "状态", nullable = true, example = "12345")
    private Integer state;
    /**
     * 短信通道
     */
    @Schema(description = "短信通道", nullable = true, example = "12345")
    private String smsType;
    /**
     * 使用时间
     */
    @Schema(description = "使用时间", nullable = true, example = "12345")
    private LocalDateTime useTime;
    /**
     * 发送结果
     */
    @Schema(description = "发送结果", nullable = true, example = "12345")
    private String sendResult;
    /**
     * 添加时间
     */
    @Schema(description = "添加时间", nullable = true, example = "12345")
    private LocalDateTime addTime;
}
