package com.chinaunicom.smartstorage.admin.service.inter.aggregation.ai;

import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.ventilation.VentilationPlanReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.ventilation.VentilationPlanRespDTO;

/**
 * AI辅助 - 通风作业智能辅助算法服务
 * <p>算法依据：LS/T 1202-2002《储粮机械通风技术规程》</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
public interface VentilationAiService {

    /**
     * 生成通风方案（可行性判定 + 风网参数计算 + 通风时段推荐）
     *
     * @param reqDTO 请求参数
     * @return 方案结果
     * @author smart-storage
     * @since 2026/10/21
     */
    VentilationPlanRespDTO generatePlan(VentilationPlanReqDTO reqDTO);

    /**
     * 单点通风条件判定（供虫害分析方案B等模块复用）
     *
     * @param grainType 粮种
     * @param grainTempC 粮温 ℃
     * @param grainMoisture 粮食水分 %
     * @param airTempC 大气温度 ℃
     * @param airRh 大气相对湿度 %
     * @return true=允许降温通风
     * @author smart-storage
     * @since 2026/10/21
     */
    boolean allowCoolingAeration(String grainType, double grainTempC, double grainMoisture,
        double airTempC, double airRh);
}
