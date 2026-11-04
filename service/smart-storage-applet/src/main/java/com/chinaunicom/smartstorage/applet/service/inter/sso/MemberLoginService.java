package com.chinaunicom.smartstorage.applet.service.inter.sso;

import com.chinaunicom.smartstorage.applet.dto.sso.*;
import com.chinaunicom.smartstorage.applet.dto.sso.*;
import com.chinaunicom.smartstorage.common.result.ActionResult;

/**
 * @author liuchink
 * @since 2023/11/4 15:17
 */
public interface MemberLoginService {

    ActionResult<WechatMiniLoginRespDTO, String> loginByOpenid(OpenidLoginReqDTO openidLoginReqDTO);

    ActionResult<LoginRespDTO, String> bindMemberByPhone(BindMemberByPhoneReqDTO bindMemberByPhoneReqDTO);

    ActionResult<LoginRespDTO, String> loginByPassword(PasswordLoginRepDTO passwordLoginRepDTO);

    ActionResult<String, String> forgetPassword(ForgetPasswordReqDTO forgetPasswordReqDTO);

}
