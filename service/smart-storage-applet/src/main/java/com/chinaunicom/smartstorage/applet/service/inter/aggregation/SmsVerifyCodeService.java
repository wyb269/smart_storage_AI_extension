package com.chinaunicom.smartstorage.applet.service.inter.aggregation;

import com.chinaunicom.smartstorage.common.result.ActionResult;

/**
 * 验证码
 *
 * @author liuchink
 * @since 2022/1/30 9:25
 */
public interface SmsVerifyCodeService {
    /**
     *
     * 生成验证码
     *
     * @param phone phone
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2022/1/30 9:18
     */
    ActionResult<String, String> createSmsVerifyCode(String phone);

    /**
     *
     * 验证码验证
     *
     * @param phone phone
     * @param code code
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2022/1/30 9:20
     */
    ActionResult<String, String> checkSmsVerifyCode(String phone, String code);

}
