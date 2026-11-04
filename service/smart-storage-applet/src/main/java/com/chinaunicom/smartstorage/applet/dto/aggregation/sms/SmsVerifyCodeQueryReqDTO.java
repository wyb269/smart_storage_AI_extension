package com.chinaunicom.smartstorage.applet.dto.aggregation.sms;



import com.chinaunicom.smartstorage.common.bean.BaseQueryDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 手机验证码
 *
 * @author liuchink
 * @since 2022/1/30 9:17
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SmsVerifyCodeQueryReqDTO extends BaseQueryDTO  {

    /**
     * 手机号码
     */
    @Schema(description = "手机号码", nullable = true, example = "12345")
    private String phone;

}
