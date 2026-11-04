package com.chinaunicom.smartstorage.applet.service.inter.member;

import com.chinaunicom.smartstorage.applet.dto.member.MemberUpdatePasswordReqDTO;
import com.chinaunicom.smartstorage.applet.entity.member.Member;
import com.chinaunicom.smartstorage.common.result.ActionResult;

/**
 * 会员
 *
 * @author liuchink
 * @since 2023/11/4 14:11
 */
public interface MemberService {

    ActionResult<String, String> updateMember(Member member);

    ActionResult<String, String> updateMemberPhone(Member member);

    Member displayMemberByWechatMiniOpenid(String wechatMiniOpenid);

    Member displayMemberById(String id);

    ActionResult<String, String> updateMemberLoginPassword(MemberUpdatePasswordReqDTO memberUpdatePasswordReqDTO);
}
