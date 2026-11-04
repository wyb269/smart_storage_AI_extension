package com.chinaunicom.smartstorage.admin.component;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.dto.sso.CheckUserPasswordReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.admin.mapper.rbac.UserMapper;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import jakarta.annotation.Resource;

/**
 * @author liuchink
 * @since 2024/9/29 14:37
 */
@Component
public class PwdComponent {
    @Resource
    private SystemConfigComponent systemConfigComponent;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private UserMapper userMapper;
    @Resource
    private ActionResultService actionResultService;

    public String getRsaDecryptAndEncryptMd5(String pwd) {
        final RSA rsa = SecureUtil.rsa(systemConfigComponent.getRsaPrivateKey().getSycValue(),
            systemConfigComponent.getRsaPublicKey().getSycValue());
        return SecureUtil.md5(SystemConstant.MD5_SOLT + rsa.decryptStr(pwd, KeyType.PrivateKey));
    }

    public String getRsaDecrypt(String pwd) {
        final RSA rsa = SecureUtil.rsa(systemConfigComponent.getRsaPrivateKey().getSycValue(),
            systemConfigComponent.getRsaPublicKey().getSycValue());
        return rsa.decryptStr(pwd, KeyType.PrivateKey);
    }

    public String getEncryptMd5(String pwd) {
        return SecureUtil.md5(SystemConstant.MD5_SOLT + pwd);
    }

    public ActionResult<String, String> checkUserLoginPassword(CheckUserPasswordReqDTO checkUserPasswordReqDTO) {
        final User user = jwtTokenComponent.getUser();
        String pwd = getRsaDecryptAndEncryptMd5(checkUserPasswordReqDTO.getLoginPwd());
        final User userFind = userMapper.selectOne(
            Wrappers.lambdaQuery(User.class).eq(User::getLoginId, user.getLoginId()).eq(User::getLoginPwd, pwd));
        if (userFind != null) {
            return actionResultService.callBackResult();
        }
        return actionResultService.callBackResultFail("密码错误");
    }
}
