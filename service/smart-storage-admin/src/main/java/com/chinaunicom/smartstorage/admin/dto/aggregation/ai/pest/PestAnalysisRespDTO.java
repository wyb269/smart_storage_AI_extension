package com.chinaunicom.smartstorage.admin.dto.aggregation.ai.pest;

import lombok.Data;

import java.util.List;

/**
 * 虫害分析 - 分析响应（含 A/B/C 方案对比）
 * <p>对应原型「分析结果 & 处置建议」：虫害风险等级 + 风险项详情 + 推荐防治方案（推荐指数、预计效果、
 * 药剂用量、作业周期、预估成本）+ 备选方案。</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
@Data
public class PestAnalysisRespDTO {

    /** 风险分（与粮情分析同口径） */
    private Double riskScore;

    /** 是否需要用户补充参数（AI 对话式追问，true 时其余分析字段为空） */
    private Boolean requireMoreInput = false;

    /** 缺失参数中文名列表（requireMoreInput=true 时有值） */
    private List<String> missingParams;

    /** AI 追问文案（requireMoreInput=true 时有值，前端流式回显） */
    private String aiMessage;

    /** 风险等级：LOW/MEDIUM/HIGH */
    private String riskLevel;

    /** 风险等级文案：低风险/中风险/高风险 */
    private String riskLevelText;

    /** 风险摘要（原型等级卡片下方一句话说明） */
    private String summary;

    /** 风险项详情（原型「风险项详情」列表，如虫口密度严重超标/环境适宜虫害繁殖/仓库密封性一般） */
    private List<RiskItem> riskItems;

    /** 虫粮等级：NONE基本无虫/GENERAL一般/SEVERE严重/DANGEROUS危险 */
    private String grainPestGrade;

    /** 等级判定依据 */
    private String gradeBasis;

    /** 处理时限提示 */
    private String treatDeadline;

    /** 抗性分组 A/B/C */
    private String resistanceGroup;

    /** 推荐方案列表（A熏蒸 / B低温通风 / C物理防治） */
    private List<Plan> plans;

    /** 防治后监测建议 */
    private MonitorPlan monitorPlan;

    @Data
    public static class RiskItem {
        /** 风险类型：PEST_DENSITY / ENVIRONMENT / SEALING / RESISTANCE / HISTORY / CLEANLINESS / FACILITY / IMPURITY */
        private String type;
        /** 风险项名称 */
        private String name;
        /** 等级：NORMAL/MILD/MODERATE/SEVERE */
        private String level;
        /** 等级文案：正常/一般/较重/严重 */
        private String levelText;
        /** 描述（含数值与超标依据） */
        private String description;
        /** 处置建议 */
        private String suggestion;
        /** 备注标签（原型右侧「注明/需注意」） */
        private String remark;
    }

    @Data
    public static class Plan {
        private String code;
        private String name;
        private Boolean recommended;
        private Boolean feasible;
        private String feasibilityNote;
        private String summary;
        /** 推荐指数（原型 ★★★★，1~5 星） */
        private Integer starRating;
        /** 预计效果说明（原型「预计效果 虫口密度降至0.5头/kg，防治有效率95%」） */
        private String effectDescription;
        /** 方案A：熏蒸结果（复用熏蒸算法） */
        private FumigationBrief fumigation;
        private Double targetTemp;
        private Integer totalDays;
        private Double effectRate;
        private Double costEstimate;
        private List<String> advantages;
        private List<String> disadvantages;
        private String detailLink;
    }

    @Data
    public static class FumigationBrief {
        private Double totalDose;
        private Integer applicationPoints;
        private Integer sealDays;
        private Integer aerateDays;
        private Double targetConcentration;
        /** 粮堆单位用药量（g/m³，原型「磷化铝6g/m³」） */
        private Double dosePerVolume;
        /** 药剂名称与规格（原型「磷化铝(56%)」） */
        private String agentName;
        private String schemeName;
    }

    @Data
    public static class MonitorPlan {
        private Integer recheckDays;
        private List<String> items;
    }
}
