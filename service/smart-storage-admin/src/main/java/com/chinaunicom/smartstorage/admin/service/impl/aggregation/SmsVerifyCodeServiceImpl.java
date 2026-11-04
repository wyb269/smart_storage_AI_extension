package com.chinaunicom.smartstorage.admin.service.impl.aggregation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.dto.aggregation.sms.SmsVerifyCodeQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.sms.SmsVerifyCode;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.SmsVerifyCodeMapper;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.SmsVerifyCodeService;
import com.chinaunicom.smartstorage.common.constant.SmsChannelConstant;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.RandomUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 验证码
 *
 * @author liuchink
 * @since 2022/1/30 9:26
 */
@Service

@Slf4j
public class SmsVerifyCodeServiceImpl implements SmsVerifyCodeService {
    private static final String SMS_VERIFY_LOCK = "sms:verify:lock:";
    private static final int SMS_VERIFY_EXPIRE_TIME = 5 * 60;
    @Resource
    private SmsVerifyCodeMapper smsVerifyCodeMapper;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> createSmsVerifyCode(String phone) {
        String lockKey = SMS_VERIFY_LOCK + phone;
        if (redisTemplate.hasKey(lockKey)) {
            return actionResultService.callBackResultFail("发送频率过快，请稍后重试");
        }
        SmsVerifyCode smsVerifyCodeByPhone = smsVerifyCodeMapper.selectById(phone);
        SmsVerifyCode smsVerifyCode = new SmsVerifyCode();
        smsVerifyCode.setPhone(phone);
        smsVerifyCode.setSmsType(SmsChannelConstant.SMS_TYPE_ALI);
        smsVerifyCode.setCode(RandomUtil.randomNumbers(6));
        smsVerifyCode.setDetails(smsVerifyCode.getCode());
        smsVerifyCode.setExpiredTime(LocalDateTime.now().plusSeconds(SMS_VERIFY_EXPIRE_TIME));
        smsVerifyCode.setState(2);
        smsVerifyCode.setAddTime(LocalDateTime.now());
        smsVerifyCode.setSendResult(SystemConstant.SUCCESS_STRING_SUCCESS);
        if (smsVerifyCodeByPhone == null) {
            smsVerifyCodeMapper.insert(smsVerifyCode);
        } else {
            smsVerifyCodeMapper.updateById(smsVerifyCode);
        }
        redisTemplate.opsForValue().set(lockKey, phone, 60, TimeUnit.SECONDS);
        return actionResultService.callBackResult();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> checkSmsVerifyCode(String phone, String code) {
        SmsVerifyCode smsVerifyCodeByPhone = smsVerifyCodeMapper.selectById(phone);
        if (smsVerifyCodeByPhone != null) {
            if (!smsVerifyCodeByPhone.getCode().equals(code) || smsVerifyCodeByPhone.getState() == 1) {
                return actionResultService.callBackResultFail("验证码错误");
            }
            if (smsVerifyCodeByPhone.getExpiredTime().isBefore(LocalDateTime.now())) {
                return actionResultService.callBackResultFail("验证码已过期");
            }
            smsVerifyCodeByPhone.setState(1);
            smsVerifyCodeByPhone.setUseTime(LocalDateTime.now());
            smsVerifyCodeMapper.updateById(smsVerifyCodeByPhone);
            return actionResultService.callBackResult("验证通过");
        } else {
            return actionResultService.callBackResultFail("手机号码不存在");
        }
    }

    @Override
    public List<SmsVerifyCode> listSmsVerifyCodeByConditon(SmsVerifyCodeQueryReqDTO smsVerifyCodeQueryReqDTO) {
        return smsVerifyCodeMapper.selectList(Wrappers.lambdaQuery(SmsVerifyCode.class)
            .like(CharSequenceUtil.isNotBlank(smsVerifyCodeQueryReqDTO.getPhone()), SmsVerifyCode::getPhone,
                smsVerifyCodeQueryReqDTO.getPhone())
            .orderByDesc(SmsVerifyCode::getAddTime).orderByDesc(SmsVerifyCode::getPhone));
    }
}
