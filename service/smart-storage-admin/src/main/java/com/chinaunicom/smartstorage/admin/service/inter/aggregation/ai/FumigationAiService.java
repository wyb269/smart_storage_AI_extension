package com.chinaunicom.smartstorage.admin.service.inter.aggregation.ai;

import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation.FumigationPlanReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation.FumigationPlanRespDTO;

/**
 * AI辅助 - 熏蒸作业智能辅助算法服务
 * <p>算法依据：LS/T 1201-2020《磷化氢熏蒸技术规程》、GB/T 29890-2013、GB/T 22497</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
public interface FumigationAiService {

    /**
     * 生成熏蒸方案（抗性分组、最低有效浓度查表、设定浓度、剂量计算、时间线）
     *
     * @param reqDTO 请求参数
     * @return 方案结果
     * @author smart-storage
     * @since 2026/10/21
     */
    FumigationPlanRespDTO generatePlan(FumigationPlanReqDTO reqDTO);

    /**
     * 熏蒸方案核心计算（不做参数完整性检查）
     * <p>供虫害分析模块内部联动调用：内部构造的请求仅含熏蒸剂量计算所需字段，
     * 作业信息/气体参数/药剂选择按规程推荐值执行，不走 AI 追问。</p>
     *
     * @param reqDTO 请求参数
     * @return 方案结果
     * @author smart-storage
     * @since 2026/10/29
     */
    FumigationPlanRespDTO generatePlanCore(FumigationPlanReqDTO reqDTO);

    /**
     * 害虫抗性分组（供虫害分析模块复用）
     *
     * @param pestSpecies 虫种编码列表
     * @param resistanceLevel 用户输入的抗药性（可为null）
     * @return A / B / C
     * @author smart-storage
     * @since 2026/10/21
     */
    String resolveResistanceGroup(java.util.List<String> pestSpecies, String resistanceLevel);
}
