package com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation;

import lombok.Data;

import java.util.List;

/**
 * 熏蒸辅助 - 生成方案响应
 * <p>对应原型「最优熏蒸方案」：方案标题+一句话说明、关键指标卡（药剂总量/投药点数/密闭熏蒸/散气排毒）、
 * 作业时间安排、详细操作步骤、安全警示。</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
@Data
public class FumigationPlanRespDTO {

    /** 方案名称（原型标题，如「磷化铝熏蒸方案」） */
    private String schemeName;

    /** 是否需要用户补充参数（AI 对话式追问，true 时其余方案字段为空） */
    private Boolean requireMoreInput = false;

    /** 缺失参数中文名列表（requireMoreInput=true 时有值） */
    private List<String> missingParams;

    /** AI 追问文案（requireMoreInput=true 时有值，前端流式回显） */
    private String aiMessage;

    /** 方案一句话说明（原型标题下灰字，如「针对5号平房仓玉米，虫口密度8.5头/kg，严重危害…」） */
    private String summary;

    /** 仓库名称/仓号回显 */
    private String warehouseName;

    /** 是否可行 */
    private Boolean feasible;

    /** 不可行原因 */
    private List<String> rejectedReasons;

    /** 人工干预项说明（如「目标浓度、密闭天数由人工指定」），为空表示全部按规程推荐 */
    private List<String> manualOverrides;

    /** 害虫抗性分组：A敏感 / B抗性 / C强抗性 */
    private String resistanceGroup;

    /** 药剂类型：ALUMINIUM_PHOSPHIDE磷化铝 / MAGNESIUM_PHOSPHIDE磷化镁 */
    private String agentType;

    /** 药剂名称（原型「磷化铝片剂」） */
    private String agentName;

    /** 药剂规格（原型「56%」） */
    private String agentSpec;

    /** 片剂单重（g/片） */
    private Double tabletWeight;

    /** 设定浓度（mL/m³） */
    private Double targetConcentration;

    /** 规程最低有效浓度（mL/m³） */
    private Double minEffectiveConcentration;

    /** 实际 CT 值（ppm·h）= 设定浓度 × 密闭天数 × 24 */
    private Double ctValue;

    /** 用户要求/规程建议的 CT 值（ppm·h） */
    private Double ctRequired;

    /** 密闭天数决策依据 */
    private String sealDaysBasis;

    /** 用药量计算 */
    private DoseInfo dose;

    /** 时间安排 */
    private ScheduleInfo schedule;

    /** 浓度检测计划 */
    private List<ConcCheck> concentrationCheck;

    /** 气体取样点数 */
    private Integer samplingPoints;

    /** 详细操作步骤（原型「详细操作步骤」1~5 步） */
    private List<OperationStep> operationSteps;

    /** 安全操作提示（原型「安全警示（务必遵守）」） */
    private List<String> safetyTips;

    @Data
    public static class DoseInfo {
        /** 粮堆单位用药量（g/m³） */
        private Double dosePerVolumePile;
        /** 空间单位用药量（g/m³） */
        private Double dosePerVolumeSpace;
        /** 粮堆用药量（kg） */
        private Double dosePile;
        /** 空间用药量（kg） */
        private Double doseSpace;
        /** 总药量（kg） */
        private Double totalDose;
        /** 片数 */
        private Integer tabletCount;
        /** 投药点数 */
        private Integer applicationPoints;
        /** 每点最大药量（g） */
        private Double maxPerPoint;
        /** 投药点分布明细（原型「粮面3处+探管3处」） */
        private List<PointDetail> pointDetails;
    }

    @Data
    public static class PointDetail {
        /** 位置名称：粮面 / 探管 / 环流管道 */
        private String location;
        /** 点位数 */
        private Integer points;
        /** 每点药量（g） */
        private Double dosePerPoint;
    }

    @Data
    public static class ScheduleInfo {
        /** 是否含 D1 投药日，总作业周期（天） */
        private Integer totalDays;
        /** 密闭熏蒸天数（≥14） */
        private Integer sealDays;
        /** 散气排毒天数 */
        private Integer aerateDays;
        /** 散气目标浓度（mL/m³，低于该值方可进仓，原型「降至0.3ppm以下」） */
        private Double aerationTargetConcentration;
        /** 时间安排 D1~D19+ */
        private List<TimelineItem> timeline;
    }

    @Data
    public static class TimelineItem {
        private String phase;
        private String name;
        private String desc;
    }

    @Data
    public static class ConcCheck {
        private String time;
        private String require;
    }

    @Data
    public static class OperationStep {
        /** 步骤序号 */
        private Integer stepNo;
        /** 步骤标题，如「熏蒸前准备」 */
        private String title;
        /** 步骤内容 */
        private String content;
    }
}
