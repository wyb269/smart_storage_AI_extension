package com.chinaunicom.smartstorage.applet.mapper.member;

import org.apache.ibatis.annotations.Param;

import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.applet.entity.member.Member;

/**
 * 会员
 *
 * @author liuchink
 * @since 2022/6/29 16:56
 */

public interface MemberMapper extends MPJBaseMapper<Member> {
    Member displayMemberByWechatMiniOpenid(@Param("wechatMiniOpenid") String wechatMiniOpenid);

    Member displayMemberById(@Param("id") String openId);

}
