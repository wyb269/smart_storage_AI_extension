package com.chinaunicom.smartstorage.admin.service.inter.aggregation.ai;

import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.pest.PestAnalysisReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.pest.PestAnalysisRespDTO;

/**
 * AI辅助 - 虫害分析算法服务
 * <p>算法依据：GB/T 29890-2013（虫粮等级）、LS/T 1201-2020、LS/T 1202-2002</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
public interface PestAnalysisAiService {

    /**
     * 虫害分析：虫粮等级判定 + 风险评分 + A/B/C 方案生成与比选
     *
     * @param reqDTO 请求参数
     * @return 分析结果
     * @author smart-storage
     * @since 2026/10/21
     */
    PestAnalysisRespDTO analyze(PestAnalysisReqDTO reqDTO);
}
