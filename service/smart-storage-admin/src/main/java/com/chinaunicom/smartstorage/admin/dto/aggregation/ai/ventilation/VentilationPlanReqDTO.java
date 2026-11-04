package com.chinaunicom.smartstorage.admin.dto.aggregation.ai.ventilation;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

/**
 * 通风辅助 - 生成方案请求
 * <p>对外模拟大模型：不做接口级硬校验，参数缺失由服务入口检查后以 AI 口吻追问</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
@Data
public class VentilationPlanReqDTO {

    /**
     * 通风目标（多选）：COOLING降温 / MOISTURE_REDUCTION降水 / CONDITIONING调质 / BALANCE平衡粮温防结露
     */
    private List<String> ventilationGoals;

    @Valid
    private GrainInfo grainInfo;

    @Valid
    private DeviceInfo deviceInfo;

    /**
     * 未来逐时天气预报
     */
    @Valid
    private List<WeatherPoint> weatherForecast;

    /**
     * 是否亚热带地区（影响温度阈值 8/6℃、4/3℃）
     */
    private boolean subtropical = false;

    /**
     * 目标粮温（℃），降温/平衡通风的达标目标
     */
    private Double targetGrainTemp;

    /**
     * 目标水分（%），降水/调质通风的达标目标
     */
    private Double targetMoisture;

    /**
     * 仓内温度（℃）
     */
    private Double warehouseTemp;

    /**
     * 仓内湿度（%）
     */
    private Double warehouseRh;

    /**
     * 约束条件（允许通风时间窗、最长连续时长、避雨湿、峰谷电价、自然通风）
     */
    @Valid
    private ConstraintInfo constraintInfo;

    /**
     * 历史粮情与通风效果
     */
    @Valid
    private HistoryInfo historyInfo;

    @Data
    public static class GrainInfo {
        /** 仓型：FLAT房式仓 / SHALLOW浅圆仓 / SILO立筒仓 */
        private String warehouseType;
        /** 粮种：WHEAT/CORN/PADDY/RICE/SOYBEAN/BARLEY/PEANUT */
        private String grainType;
        /** 粮食质量 G（t） */
        private Double grainWeight;
        /** 粮堆体积 V（m³） */
        private Double grainVolume;
        /** 容重 r（t/m³） */
        private Double bulkDensity;
        /** 粮食水分 w（%） */
        private Double grainMoisture;
        /** 当地储粮安全水分（%） */
        private Double safeMoisture;
        /** 粮堆高度 h（m） */
        private Double pileHeight;
        /** 粮堆平均温度 t2（℃） */
        private Double grainAvgTemp;
        /** 粮堆上/下层温差（℃） */
        private Double topBottomTempDiff;
        /** 粮堆温度梯度（℃/m） */
        private Double tempGradient;
        /** 粮堆水分梯度（%/m） */
        private Double moistureGradient;
        /** 粮堆上/下层水分差（%） */
        private Double topBottomMoistureDiff;
    }

    @Data
    public static class DeviceInfo {
        /** 风机类型：CENTRIFUGAL离心 / AXIAL轴流 / MIXED混流 / EXHAUST排风扇 */
        private String fanType;
        /** 送风方式：PRESSURE压入 / SUCTION吸出 / MIXED混合 */
        private String airSupplyMode;
        /** 单台风机风量（m³/h），与风机数量合成设备总风量 */
        private Double fanAirflowPerUnit;
        /** 风机数量（台） */
        private Integer fanCount;
        /** 单台风机功率（kW） */
        private Double fanPower;
        /** 主风道横截面积 F主（m²） */
        private Double mainDuctArea;
        /** 支风道横截面积（m²） */
        private Double branchDuctArea;
        /** 现有风道间距（m） */
        private Double ductSpacing;
        /** 压紧系数 φ（1.30~1.55） */
        private Double compactionFactor;
    }

    @Data
    public static class WeatherPoint {
        /** 时间，格式 yyyy-MM-dd'T'HH:mm:ss */
        private String time;
        /** 气温 ℃ */
        private Double temp;
        /** 相对湿度 % */
        private Double rh;
        /** 天气：晴 / 阴 / 雨（RAIN），约束“避开雨天及高湿天气”时使用 */
        private String weather;
    }

    @Data
    public static class ConstraintInfo {
        /** 允许通风时间窗，格式 HH:mm-HH:mm，支持跨零点，如 20:00-08:00；多段用列表 */
        private List<String> allowTimeRanges;
        /** 只利用自然通风（零能耗，不启动风机） */
        private Boolean naturalOnly = false;
        /** 避开雨天及高湿天气（相对湿度>85%） */
        private Boolean avoidRainHumidity = false;
        /** 考虑峰谷电价（成本优先，优先推荐谷段时段） */
        private Boolean peakValleyPricing = false;
        /** 最长连续通风时长（h），超出自动切分 */
        private Double maxContinuousHours;
    }

    @Data
    public static class HistoryInfo {
        /** 去年同期粮温（℃） */
        private Double lastYearGrainTemp;
        /** 去年同期水分（%） */
        private Double lastYearMoisture;
        /** 近一年通风次数 */
        private Integer ventilationTimesLastYear;
        /** 上次通风类型：降温 / 降水 / 调质 / 环流 */
        private String lastVentilationType;
        /** 上次通风时长（h） */
        private Double lastVentilationHours;
        /** 通风前粮温（℃） */
        private Double lastTempBefore;
        /** 通风后粮温（℃） */
        private Double lastTempAfter;
        /** 历史通风效果评价：GOOD良好 / MEDIUM一般 / BAD较差（兼容中文） */
        private String lastEffectRating;
        /** 上次通风能耗（kWh） */
        private Double lastEnergyKwh;
    }
}
