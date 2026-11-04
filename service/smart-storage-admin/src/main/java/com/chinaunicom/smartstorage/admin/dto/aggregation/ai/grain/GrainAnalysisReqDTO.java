package com.chinaunicom.smartstorage.admin.dto.aggregation.ai.grain;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

/**
 * 粮情分析 - 分析请求
 * <p>对应原型「粮情智能分析」输入区 5 组参数：仓库信息 / 粮温参数 / 仓内环境 / 外界气象条件 / 预测周期。
 * 对外模拟大模型：不做接口级硬校验，参数缺失由服务入口检查后以 AI 口吻追问。</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
@Data
public class GrainAnalysisReqDTO {

    @Valid
    private WarehouseInfo warehouseInfo;

    @Valid
    private GrainInfo grainInfo;

    @Valid
    private MonitorData monitorData;

    @Valid
    private EnvData envData;

    @Valid
    private HistoryInfo historyInfo;

    /**
     * 预测周期（天，原型「未来七天/未来15天/未来30天」）：7 / 15 / 30，默认 15
     */
    private Integer forecastDays;

    @Data
    public static class WarehouseInfo {
        /** 仓库ID（原型「选择仓库」必填项） */
        private String warehouseId;
        /** 仓库名称/仓号，用于结论回显 */
        private String warehouseName;
        /** 仓房类型：FLAT平房仓 / SHALLOW浅圆仓 / SILO立筒仓（可由仓库档案带出） */
        private String warehouseType;
        /** 仓容（吨） */
        private Double capacity;
        /** 现存粮食数量（t） */
        private Double storedWeight;
        /** 入仓日期 yyyy-MM-dd */
        private String storedDate;
    }

    @Data
    public static class GrainInfo {
        /** 粮种 */
        private String grainType;
        /** 粮食水分（%） */
        private Double grainMoisture;
        /** 当地储粮安全水分（%） */
        private Double safeMoisture;
        /** 平均粮温（℃） */
        private Double grainAvgTemp;
        /** 最高粮温（℃，原型「最高粮温」） */
        private Double grainMaxTemp;
        /** 最低粮温（℃） */
        private Double grainMinTemp;
        /** 上层平均粮温（℃，原型「上层平均」） */
        private Double topLayerTemp;
        /** 中层平均粮温（℃，原型「中层平均」） */
        private Double middleLayerTemp;
        /** 下层平均粮温（℃，原型「下层平均」） */
        private Double bottomLayerTemp;
        /** 杂质含量（%） */
        private Double impurity;
        /** 粮堆高度（m） */
        private Double pileHeight;
    }

    @Data
    public static class MonitorData {
        /** 异常高温点数量（高于周边3℃以上） */
        private Integer hotSpotCount = 0;
        private Double hotSpotMaxTemp;
        /** 近7天异常点升温速率（℃/天） */
        private Double tempRiseRate;
        private Double topBottomTempDiff;
        private Double topBottomMoistureDiff;
        /** 害虫总密度（头/kg，原型「害虫密度(头/kg)」） */
        private Double pestDensity;
        /** 主要害虫密度（头/kg） */
        private Double mainPestDensity;
        private List<String> pestSpecies;
    }

    @Data
    public static class EnvData {
        /** 仓温（℃，原型「仓温(℃)」） */
        private Double warehouseTemp;
        /** 仓湿（%，原型「仓湿(%)」） */
        private Double warehouseRh;
        /** 室外温度（℃，原型「室外温度(℃)」） */
        private Double airTemp;
        /** 室外湿度（%，原型「室外湿度(%)」） */
        private Double airRh;
        /** 季节：SPRING/SUMMER/AUTUMN/WINTER */
        private String season;
    }

    @Data
    public static class HistoryInfo {
        private String lastFumigationDate;
        private String lastPestDate;
        /** 历史问题：PEST/MOLD/CONDENSATION */
        private List<String> pastIssues;
    }
}
