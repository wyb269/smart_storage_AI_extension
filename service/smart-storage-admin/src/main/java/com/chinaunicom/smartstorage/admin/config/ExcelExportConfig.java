package com.chinaunicom.smartstorage.admin.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExcelExportConfig {
    // 核心结构
    public static class ExportConfig {
        public String aliasName; // 中文别名（导出文件名用）
        public Map<String, String> headers; // 表头

        public ExportConfig(String aliasName, Map<String, String> headers) {
            this.aliasName = aliasName;
            this.headers = headers;
        }
    }

    // ====================== 全局导出配置 ======================
    public static final Map<String, ExportConfig> config = new LinkedHashMap<>();

    static {
        // ========== 结算报表 ==========
        config.put("settlementReport", new ExportConfig(
                "结算报表",  // 【导出文件名 会自动加年月日】
                new LinkedHashMap<String, String>() {{
                    //导出的表结构
                    put("作业编号", "jobNo");
                    put("客户名称", "customerName");
                    put("粮食品种", "grainVariety");
                    put("粮食等级", "grainLevel");
                    put("毛重(KG)", "grossWeight");
                    put("皮重(KG)", "tareWeight");
                    put("净重(KG)", "netWeight");
                    put("作业类型", "jobType");
                    put("结算重量(KG)", "settleWeight");
                    put("结算单价(元)", "settleUnitPrice");
                    put("结算金额(元)", "settleAmount");
                    put("结算时间", "settleTime");
                }}
        ));


        // ========== 新业务按上面格式添加即可 ==========
        config.put("registrationReport", new ExportConfig(
                "出入库报表",  // 【导出文件名 会自动加年月日】
                new LinkedHashMap<String, String>() {{
                    //导出的表结构
                    put("作业编号", "jobNo");
                    put("作业类型", "jobType");
                    put("客户名称", "customerName");
                    put("承运人", "carrier");
                    put("车牌号", "carNo");
                    put("粮食品种","grainVariety");
                    put("仓库名称", "warehouseName");
                    put("流程状态", "jobStatus");
                    put("毛重(KG)", "grossWeight");
                    put("皮重(KG)", "tareWeight");
                    put("净重(KG)", "netWeight");
                    put("结算重量(KG)", "settleWeight");
                    put("结束时间", "endTime");
                }}
        ));
        // ========== 仓门异动事件（第29.5节 导出） ==========
        config.put("doorAnomalyEvent", new ExportConfig(
                "仓门异动记录",
                new LinkedHashMap<String, String>() {{
                    put("事件编号", "eventNo");
                    put("发生时间", "occurredAt");
                    put("仓库名称", "warehouseName");
                    put("仓门位置", "doorName");
                    put("摄像头", "cameraCode");
                    put("异动类型", "anomalyTypeName");
                    put("算法状态", "algorithmStatus");
                    put("算法结论", "algorithmResult");
                    put("置信度", "confidence");
                    put("处理状态", "processStatusName");
                    put("处理人", "processedBy");
                    put("处理时间", "processedAt");
                    put("处理意见", "processOpinion");
                }}
        ));
        // ========== 视频遮挡检测（aiVision 功能族） ==========
        // key 必须与 VideoOcclusionConstant.EXPORT_BIZ_TYPE 字面一致
        config.put("videoOcclusionEvent", new ExportConfig(
                "视频遮挡记录",
                new LinkedHashMap<String, String>() {{
                    put("事件编号", "eventNo");
                    put("发生时间", "occurredAt");
                    put("仓库名称", "warehouseName");
                    put("摄像头", "cameraCode");
                    put("摄像头位置", "cameraPosition");
                    put("遮挡类型", "occlusionTypeName");
                    put("算法结论", "algorithmResult");
                    put("置信度", "confidence");
                    put("处理状态", "processStatusName");
                    put("处理人", "processedBy");
                    put("处理时间", "processedAt");
                    put("处理意见", "processOpinion");
                }}
        ));
        // ========== 粮面虫害智能识别 ==========
        // key 必须与 PestDetectionConstant.EXPORT_BIZ_TYPE 字面一致
        config.put("pestDetectionEvent", new ExportConfig(
                "虫害检测记录",
                new LinkedHashMap<String, String>() {{
                    put("采样编号", "eventNo");
                    put("采样时间", "occurredAt");
                    put("仓库名称", "warehouseName");
                    put("粮种", "grainType");
                    put("采样位置", "samplePosition");
                    put("摄像头", "cameraCode");
                    put("虫种", "insectSpecies");
                    put("密度(头/kg)", "density");
                    put("判定阈值(头/kg)", "densityThreshold");
                    put("结论类型", "conclusionTypeName");
                    put("算法结论", "algorithmResult");
                    put("置信度", "confidence");
                    put("虫情状态", "processStatusName");
                    put("处理人", "processedBy");
                    put("处理时间", "processedAt");
                    put("处理意见", "processOpinion");
                }}
        ));
        // ========== 人员行为监测 ==========
        // key 必须与 PersonBehaviorConstant.EXPORT_BIZ_TYPE 字面一致；
        // 列顺序与 mock/personBehavior 的表头一致，改动要同步两边。
        config.put("personBehaviorEvent", new ExportConfig(
                "人员行为记录",
                new LinkedHashMap<String, String>() {{
                    put("事件编号", "eventNo");
                    put("发生时间", "occurredAt");
                    put("仓库名称", "warehouseName");
                    put("行为类型", "behaviorTypeName");
                    put("摄像头", "cameraCode");
                    put("监测区域", "areaName");
                    put("算法结论", "algorithmResult");
                    put("置信度", "confidence");
                    put("处理状态", "processStatusName");
                    put("处理人", "processedBy");
                    put("处理时间", "processedAt");
                    put("处理意见", "processOpinion");
                }}
        ));
        // ========== 皮带机下站人监测 ==========
        // key 必须与 BeltMachineConstant.EXPORT_BIZ_TYPE 字面一致；
        // 列顺序与 mock/beltMachine 的表头一致，改动要同步两边。
        config.put("beltMachineEvent", new ExportConfig(
                "皮带机闲人记录",
                new LinkedHashMap<String, String>() {{
                    put("事件编号", "eventNo");
                    put("发生时间", "occurredAt");
                    put("仓库名称", "warehouseName");
                    put("皮带机", "deviceName");
                    put("摄像头", "cameraCode");
                    put("结论类型", "conclusionTypeName");
                    put("站内人数", "personCount");
                    put("算法结论", "algorithmResult");
                    put("置信度", "confidence");
                    put("处理状态", "processStatusName");
                    put("处理人", "processedBy");
                    put("处理时间", "processedAt");
                    put("处理意见", "processOpinion");
                }}
        ));
        // ========== 地面积水监测 ==========
        // key 必须与 GroundWaterConstant.EXPORT_BIZ_TYPE 字面一致；
        // 列顺序与 mock/groundWater 的 EXPORT_HEADER 一致，改动要同步两边。
        config.put("groundWaterEvent", new ExportConfig(
                "地面积水记录",
                new LinkedHashMap<String, String>() {{
                    put("事件编号", "eventNo");
                    put("发生时间", "occurredAt");
                    put("仓库名称", "warehouseName");
                    put("监测位置", "pointName");
                    put("摄像头", "cameraCode");
                    put("积水类型", "waterTypeName");
                    put("算法结论", "algorithmResult");
                    put("置信度", "confidence");
                    put("处理状态", "processStatusName");
                    put("处理人", "processedBy");
                    put("处理时间", "processedAt");
                    put("处理意见", "processOpinion");
                }}
        ));
        // ========== 周界入侵识别 ==========
        // key 必须与 PerimeterIntrusionConstant.EXPORT_BIZ_TYPE 字面一致；
        // 列顺序与 mock/perimeterIntrusion 的 EXPORT_HEADER 一致，改动要同步两边。
        config.put("perimeterIntrusionEvent", new ExportConfig(
                "周界入侵记录",
                new LinkedHashMap<String, String>() {{
                    put("事件编号", "eventNo");
                    put("发生时间", "occurredAt");
                    put("防区", "zoneName");
                    put("防区段", "pointName");
                    put("仓库名称", "warehouseName");
                    put("摄像头", "cameraCode");
                    put("摄像头位置", "cameraPosition");
                    put("入侵类型", "intrusionTypeName");
                    put("算法结论", "algorithmResult");
                    put("置信度", "confidence");
                    put("处理状态", "processStatusName");
                    put("处理人", "processedBy");
                    put("处理时间", "processedAt");
                    put("处理意见", "processOpinion");
                }}
        ));
        // ========== 车辆智能识别 ==========
        // key 必须与 VehicleRecognitionConstant.EXPORT_BIZ_TYPE 字面一致
        config.put("vehicleRecognitionEvent", new ExportConfig(
                "车辆出入事件记录",
                new LinkedHashMap<String, String>() {{
                    put("事件编号", "eventNo");
                    put("车牌号", "plateNo");
                    put("车辆类型", "vehicleType");
                    put("车身颜色", "vehicleColor");
                    put("行驶方向", "directionName");
                    put("识别地点", "gateName");
                    put("识别时间", "recognizedAt");
                    put("仓库名称", "warehouseName");
                    put("摄像头", "cameraCode");
                    put("算法状态", "algorithmStatus");
                    put("置信度", "confidence");
                    put("核实状态", "verifyStatusName");
                    put("处理人", "processedBy");
                    put("处理时间", "processedAt");
                    put("核实意见", "verifyOpinion");
                }}
        ));
    }
}
