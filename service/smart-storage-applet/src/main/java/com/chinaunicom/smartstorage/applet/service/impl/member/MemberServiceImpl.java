package com.chinaunicom.smartstorage.applet.service.impl.member;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.applet.component.PwdComponent;
import com.chinaunicom.smartstorage.applet.dto.member.MemberUpdatePasswordReqDTO;
import com.chinaunicom.smartstorage.applet.entity.member.Member;
import com.chinaunicom.smartstorage.applet.mapper.member.MemberMapper;
import com.chinaunicom.smartstorage.applet.service.inter.member.MemberService;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * @author liuchink
 * @since 2023/11/4 14:20
 */
@Service
@Slf4j
public class MemberServiceImpl implements MemberService {
    @Resource
    private MemberMapper memberMapper;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private PwdComponent pwdComponent;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateMember(Member member) {
        memberMapper.updateById(member);
        return actionResultService.callBackResult("修改成功");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateMemberPhone(Member member) {
        final Member memberByPhone =
            memberMapper.selectOne(Wrappers.lambdaQuery(Member.class).eq(Member::getPhone, member.getPhone()));
        if (memberByPhone != null && !memberByPhone.getId().equals(member.getId())) {
            return actionResultService.callBackResultFail("手机号码已存在，请使用其他号码");
        }
        memberMapper.updateById(member);
        return actionResultService.callBackResult("修改成功");
    }

    @Override
    public Member displayMemberByWechatMiniOpenid(String wechatMiniOpenid) {
        return memberMapper.displayMemberByWechatMiniOpenid(wechatMiniOpenid);
    }

    @Override
    public Member displayMemberById(String id) {
        return memberMapper.displayMemberById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String>
        updateMemberLoginPassword(MemberUpdatePasswordReqDTO memberUpdatePasswordReqDTO) {
        ActionResult<String, String> actionResult;
        if (StrUtil.equals(memberUpdatePasswordReqDTO.getLoginPassword(),
            memberUpdatePasswordReqDTO.getLoginPasswordSecond())) {
            Member memberById = memberMapper.selectById(memberUpdatePasswordReqDTO.getId());
            if (memberById != null) {
                if (StrUtil.equals(memberUpdatePasswordReqDTO.getOldPassword(), memberById.getPassword())) {
                    Member member = new Member();
                    member.setId(memberById.getId());
                    member.setPassword(memberUpdatePasswordReqDTO.getLoginPassword());
                    memberMapper.updateById(member);
                    actionResult = actionResultService.callBackResult("修改密码成功");
                } else {
                    actionResult = actionResultService.callBackResultFail("原始密码错误");
                }
            } else {
                actionResult = actionResultService.callBackResultFail("原始密码错误");
            }
        } else {
            actionResult = actionResultService.callBackResultFail("两次密码输入不一致");
        }
        return actionResult;
    }
}
