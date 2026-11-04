package com.chinaunicom.smartstorage.admin.dto.aggregation.ai.grain;

import lombok.Data;

import java.util.List;

/**
 * 粮情分析 - 分析响应（风险等级 + 预警项 + 处置方案）
 * <p>对应原型「分析结果 & 处置建议」：综合风险等级（分数+一句话结论）、预警项详情、
 * 推荐处置方案（可多条，按优先级排序）。</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
@Data
public class GrainAnalysisRespDTO {

    /** 综合风险分（0~100，越高越危险） */
    private Double riskScore;

    /** 是否需要用户补充参数（AI 对话式追问，true 时其余分析字段为空） */
    private Boolean requireMoreInput = false;

    /** 缺失参数中文名列表（requireMoreInput=true 时有值） */
    private List<String> missingParams;

    /** AI 追问文案（requireMoreInput=true 时有值，前端流式回显） */
    private String aiMessage;

    /** 风险等级：LOW/MEDIUM/HIGH */
    private String riskLevel;

    /** 风险等级文案 */
    private String riskLevelText;

    /** 风险摘要（原型等级卡片下方一句话结论，含预测周期） */
    private String summary;

    /** 预测周期（天）回显 */
    private Integer forecastDays;

    /** 处置紧迫度：monitor加强监测 / plan计划处置 / immediate立即处置 */
    private String adviceLevel;

    /** 预警项详情 */
    private List<Warning> warnings;

    /** 推荐处置方案（首条，兼容旧调用；等价于 treatPlans[0]） */
    private TreatPlan treatPlan;

    /** 推荐处置方案列表（原型多条并列，按优先级排序） */
    private List<TreatPlan> treatPlans;

    @Data
    public static class Warning {
        /** 预警类型：TEMP_ABNORMAL / PEST / MOISTURE / ENVIRONMENT */
        private String type;
        private String name;
        /** 该项等级：NORMAL/MILD/MODERATE/SEVERE */
        private String level;
        /** 等级文案：正常/一般/较重/严重（原型右侧标签） */
        private String levelText;
        /** 该项得分（该项权重即满分） */
        private Double score;
        private String description;
        private String impact;
        private List<String> suggestions;
        /** 备注标签（原型右侧「需注意/注明」） */
        private String remark;
    }

    @Data
    public static class TreatPlan {
        /** 优先级序号（原型 1/2/3） */
        private Integer priority;
        /** VENTILATION通风 / FUMIGATION熏蒸 / MIXED综合 / MONITOR监测 */
        private String primary;
        private String title;
        private String reason;
        /** 处置详情（原型方案卡内具体剂量/周期等说明） */
        private String detail;
        /** 跳转功能页：/fumigation /ventilation /pestAnalysis */
        private String linkTo;
        /** 跳转按钮文案（原型「详见「熏蒸辅助」功能」） */
        private String linkLabel;
    }
}
