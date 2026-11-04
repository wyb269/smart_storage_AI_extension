package com.chinaunicom.smartstorage.applet.dto.member;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 会员
 *
 * @author liuchink
 */
@Data
public class MemberRespDTO {


    /**
     * 微信小程序openid
     */
    @Schema(description = "微信小程序openid", nullable = true, example = "12345")
    private String wechatMiniOpenid;
    /**
     * id
     */
    @Schema(description = "id", nullable = true, example = "12345")
    private String id;
    /**
     * 昵称
     */
    @Schema(description = "昵称", nullable = true, example = "12345")
    private String nickname;

    /**
     * 手机号码
     */
    @Schema(description = "手机号码", nullable = true, example = "12345")
    private String phone;

    /**
     * 帐号启用状态1=是、2=否
     */
    @Schema(description = " 帐号启用状态1=是、2=否", nullable = true, example = "12345")
    private Integer showState;

    /**
     * 注册时间
     */
    @Schema(description = "注册时间", nullable = true, example = "12345")
    private Date addTime;

    /**
     * 头像
     */
    @Schema(description = "头像", nullable = true, example = "12345")
    private String icon;


    /**
     * 用户来源
     */
    @Schema(description = "用户来源", nullable = true, example = "12345")
    private Integer sourceType;

}
