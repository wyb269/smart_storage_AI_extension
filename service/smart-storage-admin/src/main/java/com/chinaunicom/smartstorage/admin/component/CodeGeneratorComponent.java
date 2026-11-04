package com.chinaunicom.smartstorage.admin.component;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.chinaunicom.smartstorage.admin.constant.TaskFlowConstant;
import com.chinaunicom.smartstorage.common.constant.RedisConstant;
import com.liuchink.chinkutils.date.ChinkDateFormatUtils;
import com.liuchink.chinkutils.date.ChinkDateOperationUtils;
import com.liuchink.chinkutils.date.ChinkDateValueUtils;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author liugang
 * @since 2026/1/22 10:34
 */
@Component
public class CodeGeneratorComponent {
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 作业
     *
     * @return java.lang.String
     * @author liugang
     * @since 2026/1/22 13:16
     */
    public String getWorkFlow() {
        return generateCode(TaskFlowConstant.TASK_FLOW_PREFIX, RedisConstant.TASK_FLOW_INCR_KEY);
    }

    /**
     * 登出
     */
    public String getout() {
        return generateCode(TaskFlowConstant.TASK_FLOW_LOGIN_OUT_PREFIX, RedisConstant.TASK_FLOW_LOGIN_OUT_INCR_KEY);
    }

    /**
     * 登入
     *
     * @return java.lang.String
     * @author liugang
     * @since 2026/1/22 13:16
     */
    public String getWorkFlowLogin() {
        return generateCode(TaskFlowConstant.TASK_FLOW_LOGIN_PREFIX, RedisConstant.TASK_FLOW_LOGIN_INCR_KEY);
    }

    /**
     * 扦样
     *
     * @return java.lang.String
     * @author liugang
     * @since 2026/1/22 13:16
     */
    public String getWorkFlowSampleCollection() {
        return generateCode(TaskFlowConstant.TASK_FLOW_SAMPLE_COLLECTION_PREFIX,
            RedisConstant.TASK_FLOW_SAMPLE_COLLECTION_INCR_KEY);
    }

    /**
     * 质检
     *
     * @return java.lang.String
     * @author liugang
     * @since 2026/1/22 13:16
     */
    public String getWorkFlowQualityCheck() {
        return generateCode(TaskFlowConstant.TASK_FLOW_QUALITY_CHECK_PREFIX,
            RedisConstant.TASK_FLOW_QUALITY_CHECK_INCR_KEY);
    }

    /**
     * 称重
     *
     * @return java.lang.String
     * @author liugang
     * @since 2026/1/22 13:16
     */
    public String getWorkFlowWeightCheck() {
        return generateCode(TaskFlowConstant.TASK_FLOW_WEIGHT_CHECK_PREFIX,
            RedisConstant.TASK_FLOW_WEIGHT_CHECK_INCR_KEY);
    }

    /**
     * 仓库
     *
     * @return java.lang.String
     * @author liugang
     * @since 2026/1/22 13:16
     */
    public String getWorkFlowDataWarehouse() {
        return generateCode(TaskFlowConstant.TASK_FLOW_DATA_WAREHOUSE_PREFIX,
            RedisConstant.TASK_FLOW_DATA_WAREHOUSE_INCR_KEY);
    }

    /**
     * 结算
     *
     * @return java.lang.String
     * @author liugang
     * @since 2026/1/22 13:16
     */
    public String getWorkFlowSettlement() {
        return generateCode(TaskFlowConstant.TASK_FLOW_SETTLEMENT_PREFIX, RedisConstant.TASK_FLOW_SETTLEMENT_INCR_KEY);
    }

    /**
     * 智能卡
     *
     * @return java.lang.String
     * @author liugang
     * @since 2026/1/22 13:16
     */
    public String getWorkFlowSmartCard() {
        return generateCode(TaskFlowConstant.TASK_FLOW_SMART_CARD_PREFIX, RedisConstant.TASK_FLOW_SMART_CARD_INCR_KEY);
    }

    private String generateCode(String prefix, String cacheKey) {
        final String day = ChinkDateValueUtils.getNowDateTime().toString(ChinkDateFormatUtils.DATEFORMAT_YYYYMMDD);
        final String key = cacheKey + "_" + day;
        final int betweenSecond =
            ChinkDateOperationUtils.betweenSecond(new Date(), ChinkDateValueUtils.getEndTimeOfDay().toDate());
        Long increment;
        if (redisTemplate.hasKey(key)) {
            increment = redisTemplate.opsForValue().increment(key);
        } else {
            increment = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, betweenSecond, TimeUnit.SECONDS);
        }

        return prefix.toUpperCase() + day + StrUtil.fillBefore(Convert.toStr(increment), '0', 3);
    }
}
