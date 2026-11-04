package com.chinaunicom.smartstorage.admin.entity.aggregation.sms;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 手机验证码
 *
 * @author liuchink
 * @since 2022/1/30 9:17
 */
@Data
@TableName("base_sms_code")
public class SmsVerifyCode {

    /**
     * 手机号码
     */
    @TableId(type = IdType.INPUT)
    private String phone;

    /**
     * 短信内容
     */
    private String details;

    /**
     * 验证码
     */
    private String code;

    /**
     * 过期时间
     */

    private LocalDateTime expiredTime;
    /**
     * 状态
     */

    private Integer state;
    /**
     * 短信通道
     */

    private String smsType;
    /**
     * 使用时间
     */

    private LocalDateTime useTime;
    /**
     * 发送结果
     */

    private String sendResult;
    /**
     * 添加时间
     */

    private LocalDateTime addTime;
}
