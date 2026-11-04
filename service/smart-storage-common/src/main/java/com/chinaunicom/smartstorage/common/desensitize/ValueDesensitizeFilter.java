package com.chinaunicom.smartstorage.common.desensitize;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.DesensitizedUtil;
import com.alibaba.fastjson2.filter.ValueFilter;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;

/**
 * @author liugang
 * @since 2025/11/19 16:03
 */
@Slf4j
public class ValueDesensitizeFilter implements ValueFilter {
    @Override
    public Object apply(Object object, String name, Object value) {
        if (ObjectUtils.isEmpty(value) || !(value instanceof String)) {
            return value;
        }
        try {
            Field field = object.getClass().getDeclaredField(name);
            Desensitization desensitization = field.getAnnotation(Desensitization.class);
            if (String.class != field.getType() || ObjectUtils.isEmpty(desensitization)) {
                return value;
            }
            String originVal = String.valueOf(value);
            DesensitizationTypeEnum desensitizationTypeEnum = desensitization.type();
            int startInclude = desensitization.startInclude();
            int endExclude = desensitization.endExclude();
            switch (desensitizationTypeEnum) {
                // 自定义类型脱敏
                case MY_RULE:
                    return CharSequenceUtil.hide(originVal, startInclude, endExclude);
                // userId脱敏
                case USER_ID:
                    return String.valueOf(DesensitizedUtil.userId());
                // 中文姓名脱敏
                case CHINESE_NAME:
                    return DesensitizedUtil.chineseName(String.valueOf(value));
                // 身份证脱敏
                case ID_CARD:
                    return DesensitizedUtil.idCardNum(originVal, 1, 2);
                // 固定电话脱敏
                case FIXED_PHONE:
                    return DesensitizedUtil.fixedPhone(originVal);
                // 手机号脱敏
                case MOBILE_PHONE:
                    return DesensitizedUtil.mobilePhone(originVal);
                // 地址脱敏
                case ADDRESS:
                    return DesensitizedUtil.address(originVal, 8);
                // 邮箱脱敏
                case EMAIL:
                    return DesensitizedUtil.email(originVal);
                // 密码脱敏
                case PASSWORD:
                    return DesensitizedUtil.password(originVal);
                // 中国车牌脱敏
                case CAR_LICENSE:
                    return DesensitizedUtil.carLicense(originVal);
                // 银行卡脱敏
                case BANK_CARD:
                    return DesensitizedUtil.bankCard(originVal);
                default:
            }
        } catch (NoSuchFieldException noSuchFieldException) {
            return value;
        } catch (Exception exception) {
            log.error(SystemConstant.LOG_ERROR_TITLE, exception.getMessage(), exception);
        }
        return value;
    }
}
