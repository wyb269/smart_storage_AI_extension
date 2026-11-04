package com.chinaunicom.smartstorage.applet.dto.wechat;

import com.alibaba.fastjson2.annotation.JSONField;

import lombok.Data;
import lombok.ToString;

/**
 * @author liuchink
 * @since 2021/12/28 10:59
 */
@Data
@ToString(callSuper = true)
public class WeChatMiniGetUserPhoneNumberWatermarkRespDTO {
    @JSONField(name = "timestamp")
    private String timestamp;
    @JSONField(name = "appid")
    private String appid;
}
