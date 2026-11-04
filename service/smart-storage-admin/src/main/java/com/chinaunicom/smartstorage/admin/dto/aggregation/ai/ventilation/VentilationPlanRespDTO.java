package com.chinaunicom.smartstorage.admin.dto.aggregation.ai.ventilation;

import lombok.Data;

import java.util.List;

/**
 * 通风辅助 - 生成方案响应
 *
 * @author smart-storage
 * @since 2026/10/21
 */
@Data
public class VentilationPlanRespDTO {

    /** 方案名称 */
    private String schemeName;

    /** 是否需要用户补充参数（AI 对话式追问，true 时其余方案字段为空） */
    private Boolean requireMoreInput = false;

    /** 缺失参数中文名列表（requireMoreInput=true 时有值） */
    private List<String> missingParams;

    /** AI 追问文案（requireMoreInput=true 时有值，前端流式回显） */
    private String aiMessage;

    /** 是否满足通风条件 */
    private Boolean feasible;

    /** 不可行原因汇总 */
    private List<String> rejectedReasons;

    /** 单位通风量判定 */
    private CheckItem unitAirflow;

    /** 总通风量 Q总 = q·V·r（m³/h，式1） */
    private Double totalAirflow;

    /** 风机风量 = S1·Q总（式7） */
    private Double fanAirflow;

    /** 风机风压 = S2·H总（Pa，式8） */
    private Double fanPressure;

    /** 通风系统总阻力判定 */
    private ResistanceCheck totalResistance;

    /** 空气途径比判定 */
    private CheckItem airPathRatio;

    /** 风道间距（式6） */
    private DuctSpacingCheck ductSpacing;

    /** 风道风速校核 */
    private DuctVelocityCheck ductVelocity;

    /** 推荐通风时段（连续达标时段合并） */
    private List<VentilationWindow> ventilationWindows;

    /** 结束通风条件（实时监测提示） */
    private List<CheckRow> stopConditions;

    /** 操作步骤指引（规程第8章） */
    private List<Step> operationSteps;

    /** 安全提示 */
    private List<String> safetyTips;

    /** 判定过程明细 */
    private List<CheckRow> judgeDetail;

    /** 方案结论（预计效果描述，对应原型“推荐方案”文案） */
    private String summary;

    /** 目标粮温（℃） */
    private Double targetGrainTemp;

    /** 目标水分（%） */
    private Double targetMoisture;

    /** 预计可达平均粮温（℃） */
    private Double estimatedTargetTemp;

    /** 推荐时段内是否可达到目标粮温 */
    private Boolean targetReached;

    /** 估算降温速率（℃/h） */
    private Double coolingRatePerHour;

    /** 估算降水速率（%/h） */
    private Double moistureDropRatePerHour;

    /** 预计累计通风时长（h） */
    private Double estimatedHours;

    /** 装机功率（kW） */
    private Double installedPower;

    /** 预计耗电量（kWh） */
    private Double estimatedEnergy;

    /** 单位能耗（降温 kWh/(t·℃)，降水 kWh/(t·%)） */
    private Double unitEnergy;

    /** 约束条件校验结果 */
    private List<CheckRow> constraintChecks;

    /** 历史通风效果与专项建议 */
    private List<String> advices;

    @Data
    public static class CheckItem {
        private Double value;
        private Double limit;
        private Boolean qualified;
        private String basis;
    }

    @Data
    public static class ResistanceCheck {
        /** 粮层阻力 H粮层 = 9.8·a·h·v^b·φ（Pa，式A.1） */
        private Double grainLayerResistance;
        /** 总阻力限值（降温按堆高查表2 / 降水调质1000Pa） */
        private Double limit;
        private Boolean qualified;
        /** 明细：a、b系数与表观风速 */
        private CheckRow detail;
    }

    @Data
    public static class DuctSpacingCheck {
        private Double recommended;
        private Boolean qualified;
    }

    @Data
    public static class DuctVelocityCheck {
        private CheckItem main;
        private CheckItem branch;
    }

    @Data
    public static class VentilationWindow {
        private String startTime;
        private String endTime;
        private Double hours;
        private Double avgTemp;
        private Double avgRh;
        /** 时段平均粮气温差 t2-t1（℃） */
        private Double tempDiff;
        private String judgement;
        /** 逐条判定依据 */
        private List<CheckRow> checkItems;
        /** 峰段小时数（08:00-11:00、18:00-23:00），峰谷电价约束用 */
        private Integer peakHours;
        /** 本时段预计粮温降幅（℃） */
        private Double tempDrop;
        /** 本时段预计耗电（kWh） */
        private Double energyKwh;
    }

    @Data
    public static class CheckRow {
        private String name;
        private String condition;
        private String actual;
        private Boolean passed;
    }

    @Data
    public static class Step {
        private Integer order;
        private String text;
    }
}
