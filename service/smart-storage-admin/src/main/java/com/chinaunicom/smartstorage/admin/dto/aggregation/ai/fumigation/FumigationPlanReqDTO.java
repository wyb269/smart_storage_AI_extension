package com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

/**
 * 熏蒸辅助 - 生成方案请求
 * <p>算法依据：LS/T 1201-2020《磷化氢熏蒸技术规程》、GB/T 29890-2013</p>
 * <p>对应原型「熏蒸作业智能辅助」输入区 5 组参数：仓库信息 / 粮情参数 / 虫情状况 / 作业信息 / 气体参数 + 药剂选择。
 * 对外模拟大模型：不做接口级硬校验，参数缺失由服务入口检查后以 AI 口吻追问。</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
@Data
public class FumigationPlanReqDTO {

    @Valid
    private WarehouseInfo warehouseInfo;

    @Valid
    private GrainInfo grainInfo;

    @Valid
    private PestInfo pestInfo;

    @Valid
    private EnvInfo envInfo;

    /** 作业信息（原型第4组） */
    @Valid
    private OperationInfo operationInfo;

    /** 气体参数（原型第5组） */
    @Valid
    private GasParamInfo gasParam;

    /** 药剂选择（原型「药剂选择」组） */
    @Valid
    private ChemicalInfo chemicalInfo;

    @Data
    public static class WarehouseInfo {
        /** 仓库ID（原型「选择仓库」下拉） */
        private String warehouseId;
        /** 仓库名称/仓号，用于方案说明回显 */
        private String warehouseName;
        /** 仓型：FLAT平房仓 / SHALLOW浅圆仓 / SILO立筒仓 */
        private String warehouseType;
        /** 仓库容量（t，原型「仓库容量(吨)」） */
        private Double capacity;
        /** 粮食数量（t） */
        private Double grainWeight;
        /** 仓房空间体积（m³，粮面以上） */
        private Double spaceVolume;
        /** 粮堆体积（m³） */
        private Double grainVolume;
        /** 粮堆高度（m，原型「粮堆高度(米)」） */
        private Double pileHeight;
        /** 仓长（m，原型「仓长(米)」） */
        private Double warehouseLength;
        /** 仓宽（m，原型「仓宽(米)」） */
        private Double warehouseWidth;
        /** 仓内气密性等级（原型「一级/二级/三级」）：LEVEL_1 / LEVEL_2 / LEVEL_3 */
        private String airtightLevel;
        /** 气密性：500Pa→250Pa 压力半衰期（s） */
        private Double airtightDecay;
        /** 密闭方式：WHOLE整仓 / UNDER_FILM膜下 / CIRCULATION环流 */
        private String sealingMethod;
        /** 是否具备环流装置 */
        private Boolean hasCirculation = false;
    }

    @Data
    public static class GrainInfo {
        /** 粮种：WHEAT/CORN/PADDY/RICE/SOYBEAN 等 */
        private String grainType;
        /** 粮食水分（%） */
        private Double grainMoisture;
        /** 杂质含量（%，原型「杂质含量(%)」） */
        private Double impurity;
        /** 害虫发生部位最低粮温（℃） */
        private Double grainTemp;
        /** 平均粮温（℃，原型「平均温度(℃)」） */
        private Double grainAvgTemp;
        /** 已储存天数 */
        private Integer storedDays;
        /** 上次熏蒸日期 yyyy-MM-dd */
        private String lastFumigationDate;
        /** 是否种子粮 */
        private Boolean seedGrain = false;
    }

    @Data
    public static class PestInfo {
        /** 虫种编码列表：CORN_WEEVIL玉米象 RICE_WEEVIL米象 GRAIN_BORER谷蠹 RED_FLOUR_BEETLE赤拟谷盗 FLAT_GRAIN_BEETLE扁谷盗 BOOK_LOUSE书虱 MOTH蛾类 等 */
        private List<String> pestSpecies;
        /** 害虫密度（头/kg） */
        private Double pestDensity;
        /** 危害等级（原型「一般/严重/特别严重」）：GENERAL / SEVERE / EXTREME */
        private String hazardLevel;
        /** 抗药性：HIGH耐药 / MEDIUM一般 / LOW敏感 */
        private String resistanceLevel;
    }

    @Data
    public static class EnvInfo {
        /** 天气：晴 / 阴 / 雨 */
        private String weather;
        /** 气温 ℃ */
        private Double airTemp;
        /** 相对湿度 % */
        private Double airRh;
    }

    @Data
    public static class OperationInfo {
        /** 作业类型：ROUTINE常规熏蒸 / EXTERNAL外包 / EMERGENCY应急 */
        private String operationType;
        /** 计划作业日期 yyyy-MM-dd */
        private String planDate;
        /** 作业负责人 */
        private String operator;
        /** 作业人数 */
        private Integer workerCount;
        /** 作业等级（原型「一般/较大/重大」）：GENERAL / MAJOR / CRITICAL */
        private String operationLevel;
    }

    @Data
    public static class GasParamInfo {
        /** 目标气体种类：PH3磷化氢 / SF硫酰氟 / CO2二氧化碳 */
        private String gasType;
        /** 目标浓度（mL/m³，ppm） */
        private Double targetConcentration;
        /** 最低有效浓度（ppm） */
        private Double minEffectiveConcentration;
        /** 浓度均匀要求（%），≥90 时自动加密取样点 */
        private Double concentrationUniformity;
        /** CT值要求（ppm·h） */
        private Double ctRequirement;
        /** 密闭天数（天） */
        private Integer sealDays;
        /** 环流等级：CIRCULATION有环流 / NATURAL无环流 */
        private String circulationLevel;
    }

    @Data
    public static class ChemicalInfo {
        /** 药剂类型：ALUMINIUM_PHOSPHIDE磷化铝 / MAGNESIUM_PHOSPHIDE磷化镁 */
        private String agentType;
        /** 施药方式（多选）：SURFACE粮面施药 / PROBE探管施药 / CIRCULATION环流熏蒸 / BAG气囊施药 */
        private List<String> applicationMethods;
        /** 历史辅助措施（多选）：CLEANING清洁卫生 / VENTILATION_PRETREAT通风降温预处理 / PEST_LINE防虫线设置 / TURNOVER翻仓倒仓 */
        private List<String> auxiliaryMeasures;
    }
}
