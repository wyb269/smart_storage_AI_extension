package com.chinaunicom.smartstorage.applet.entity.member;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员信息
 *
 * @author liuchink
 */
@Data
@TableName("ss_member")
public class Member {
    @TableId(type = IdType.INPUT)
    private String id;

    private String username;

    private String wechatMiniOpenid;

    private String password;

    private String nickname;

    private String phone;

    private Integer showState;

    private LocalDateTime addTime;

    private String icon;

    private Integer sourceType;

    private LocalDateTime lastLoginTime;
    private Integer identityType;
}
