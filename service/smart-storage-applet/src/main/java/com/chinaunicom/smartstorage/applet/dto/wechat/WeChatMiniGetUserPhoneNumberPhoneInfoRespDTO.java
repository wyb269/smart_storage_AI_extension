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
public class WeChatMiniGetUserPhoneNumberPhoneInfoRespDTO {
    @JSONField(name = "phoneNumber")
    private String phoneNumber;
    @JSONField(name = "purePhoneNumber")
    private String purePhoneNumber;
    @JSONField(name = "countryCode")
    private String countryCode;
    @JSONField(name = "watermark")
    private WeChatMiniGetUserPhoneNumberWatermarkRespDTO watermark;
}
