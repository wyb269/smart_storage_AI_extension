package com.chinaunicom.smartstorage.admin.dto.aggregation.ai.pest;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

/**
 * 虫害分析 - 分析请求
 * <p>对应原型「虫害分析」输入区 6 组参数：仓库信息 / 粮情基础参数 / 虫害现状数据 / 环境数据 /
 * 历史虫害发生记录 / 防治措施及效果。对外模拟大模型：不做接口级硬校验，参数缺失由服务入口检查后以 AI 口吻追问。</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
@Data
public class PestAnalysisReqDTO {

    @Valid
    private WarehouseInfo warehouseInfo;

    @Valid
    private GrainInfo grainInfo;

    @Valid
    private PestInfo pestInfo;

    @Valid
    private EnvData envData;

    @Valid
    private HistoryInfo historyInfo;

    @Valid
    private PreventionInfo preventionInfo;

    /** 防治措施及效果（原型第6组） */
    @Valid
    private ControlInfo controlInfo;

    @Data
    public static class WarehouseInfo {
        /** 仓库ID（原型「选择仓库」必填项） */
        private String warehouseId;
        /** 仓库名称/仓号 */
        private String warehouseName;
        /** 仓房类型：FLAT平房仓 / SHALLOW浅圆仓 / SILO立筒仓 */
        private String warehouseType;
        /** 仓容（吨） */
        private Double capacity;
        /** 现存粮食数量（t） */
        private Double storedWeight;
        /** 仓房空间体积（m³） */
        private Double spaceVolume;
        /** 粮堆体积（m³） */
        private Double grainVolume;
        /** 密闭方式：WHOLE整仓 / UNDER_FILM膜下 / CIRCULATION环流 */
        private String sealingMethod;
        /** 仓房密封性等级（原型「仓房密封性」）：LEVEL_1一级 / LEVEL_2二级 / LEVEL_3三级 */
        private String sealingLevel;
        /** 气密性：500Pa→250Pa 压力半衰期（s） */
        private Double airtightDecay;
    }

    @Data
    public static class GrainInfo {
        /** 粮种 */
        private String grainType;
        /** 粮食水分（%） */
        private Double grainMoisture;
        /** 杂质含量（%，原型「杂质含量(%)」） */
        private Double impurity;
        /** 平均粮温（℃） */
        private Double grainAvgTemp;
        /** 粮堆高度（m） */
        private Double pileHeight;
        /** 已储存天数 */
        private Integer storedDays;
        /** 储存年限（年，原型「储存年限」） */
        private Integer storedYears;
    }

    @Data
    public static class PestSpeciesItem {
        private String code;
        private Double density;
        private Boolean isMain = false;
        /** 分布：TOP/MIDDLE/BOTTOM/WHOLE */
        private String distribution;
    }

    @Data
    public static class PestInfo {
        /** 虫种列表（含密度与分布） */
        private List<PestSpeciesItem> species;
        /** 害虫总密度（头/kg） */
        private Double totalDensity;
        /** 采样点数 */
        private Integer samplePoints;
        /** 最大点密度（头/kg） */
        private Double maxPointDensity;
        /** 检测方式（原型「检测方式」）：SIEVE筛检 / TRAP诱捕器 / VISUAL目测 / TRAP_COUNT诱捕计数 */
        private String detectionMethod;
        /** 抗药性：HIGH耐药 / MEDIUM一般 / LOW敏感 */
        private String resistanceLevel;
    }

    @Data
    public static class EnvData {
        private Double airTemp;
        private Double airRh;
        /** 季节：SPRING/SUMMER/AUTUMN/WINTER */
        private String season;
        private Double warehouseTemp;
        /** 仓内湿度（%，原型「仓内湿度(%)」） */
        private Double warehouseRh;
    }

    @Data
    public static class HistoryInfo {
        private String lastFumigationDate;
        private String lastFumigationEffect;
        private String lastPestDate;
        /** 近一年发生次数 */
        private Integer recurrenceCount;
        /** 历史最高密度（头/kg，原型「历史最高密度」） */
        private Double maxHistoricalDensity;
        /** 历史主要虫种（虫种编码，原型「历史主要虫种」） */
        private String historicalMainSpecies;
    }

    @Data
    public static class PreventionInfo {
        /** 是否使用防护剂 */
        private Boolean protectantUsed = false;
        /** 是否开展清洁卫生（兼容旧字段） */
        private Boolean cleanSanitation = true;
        /** 清洁卫生等级（原型「清洁卫生等级」）：CLEAN良好 / NORMAL一般 / POOR较差 */
        private String cleaningLevel;
        /** 防虫设施（原型「防虫设施」）：COMPLETE完善 / PARTIAL部分 / NONE无 */
        private String pestFacility;
    }

    @Data
    public static class ControlInfo {
        /** 上次防治方式（原型「上次防治方式」）：FUMIGATION熏蒸 / VENTILATION通风 / PHYSICAL物理防治 / CHEMICAL防护剂 */
        private String lastControlMethod;
        /** 防治前密度（头/kg） */
        private Double densityBefore;
        /** 防治后密度（头/kg） */
        private Double densityAfter;
        /** 防治效果评价（原型「有效/部分有效/无效」）：EFFECTIVE / PARTIAL / INEFFECTIVE */
        private String effectRating;
    }
}
