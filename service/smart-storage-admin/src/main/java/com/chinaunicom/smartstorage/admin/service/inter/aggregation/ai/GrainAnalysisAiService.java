package com.chinaunicom.smartstorage.admin.service.inter.aggregation.ai;

import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.grain.GrainAnalysisReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.grain.GrainAnalysisRespDTO;

/**
 * AI辅助 - 粮情智能分析算法服务
 * <p>四维度加权评分（粮温30/虫害30/水分25/环境15），权重与阈值为建议初值，后续可配置化</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
public interface GrainAnalysisAiService {

    /**
     * 粮情综合分析：预警项评估 + 综合风险评分定级 + 处置方案推荐
     *
     * @param reqDTO 请求参数
     * @return 分析结果
     * @author smart-storage
     * @since 2026/10/21
     */
    GrainAnalysisRespDTO analyze(GrainAnalysisReqDTO reqDTO);
}
