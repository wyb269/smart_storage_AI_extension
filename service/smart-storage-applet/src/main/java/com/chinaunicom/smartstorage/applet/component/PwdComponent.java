package com.chinaunicom.smartstorage.applet.component;

import org.springframework.stereotype.Component;

import com.chinaunicom.smartstorage.applet.mapper.member.MemberMapper;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
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
    private MemberMapper memberMapper;
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
}
