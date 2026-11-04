package com.chinaunicom.smartstorage.admin.entity.aggregation.sms;

import lombok.Data;

/**
 * 手机验证码
 *
 * @author liuchink
 * @since 2022/1/30 9:17
 */
@Data
public class CreateSmsVerifyCode  {

    /**
     * 手机号码
     */
    private String phone;
    /**
     * 通道类型
     */
    private String smsType;

}
