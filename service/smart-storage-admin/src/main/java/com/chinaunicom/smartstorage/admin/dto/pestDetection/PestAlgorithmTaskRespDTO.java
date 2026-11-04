package com.chinaunicom.smartstorage.admin.dto.pestDetection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 粮面虫害算法任务响应对象（详情弹窗「算法任务」区与轮询接口共用）。
 *
 * <p>字段来自 {@code ai_pest_algorithm_task} 行；时间列在 SQL 侧统一格式化为
 * {@code yyyy-MM-dd HH:mm:ss}，与其余接口同形。
 * {@code detections} 为原始 JSON 字符串（数组元素含 category/species/count/confidence/bbox），
 * 前端仅做展示解析，永不解析 {@code rawResultRef}。</p>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
@Data
@Schema(description = "粮面虫害算法任务")
public class PestAlgorithmTaskRespDTO {

    @Schema(description = "任务标识（对外即 task_no，与算法网关任务号同源）")
    private String taskId;

    @Schema(description = "任务行主键（字符串形式，回写与排障用）")
    private String id;

    @Schema(description = "算法任务编号")
    private String taskNo;

    @Schema(description = "关联检测记录标识")
    private String eventId;

    @Schema(description = "粮面点位标识")
    private String pointId;

    @Schema(description = "摄像头业务编码")
    private String cameraCode;

    @Schema(description = "任务类型，固定 PEST_DETECT")
    private String taskType;

    @Schema(description = "执行状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED")
    private String executionStatus;

    @Schema(description = "算法结论 NORMAL/ANOMALY/UNCERTAIN")
    private String conclusion;

    @Schema(description = "标准化采样结论类型，同事件表 conclusion_type")
    private String conclusionType;

    @Schema(description = "置信度 0-1，null 表示暂无")
    private BigDecimal confidence;

    @Schema(description = "置信度阈值")
    private BigDecimal confidenceThreshold;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型版本")
    private String modelVersion;

    @Schema(description = "推理程序版本")
    private String algorithmVersion;

    @Schema(description = "配置版本")
    private String configVersion;

    @Schema(description = "开始时间")
    private String startedAt;

    @Schema(description = "完成时间；进行中/失败/超时为空")
    private String finishedAt;

    @Schema(description = "推理耗时（毫秒）")
    private Long inferenceDurationMs;

    @Schema(description = "检出目标框数量（由 detections JSON 数组长度派生）")
    private Integer detectedCount;

    @Schema(description = "检测结果 JSON 字符串（含虫种与计数），仅展示用")
    private String detections;

    @Schema(description = "受控内部媒体引用 JSON 数组（禁止任意 URL）")
    private String mediaRefs;

    @Schema(description = "是否模拟结果，恒为 true 直到真实算法接入")
    private Boolean simulated;

    @Schema(description = "失败诊断码")
    private String errorCode;

    @Schema(description = "失败诊断信息，失败/超时态必须正常展示")
    private String errorMessage;

    @Schema(description = "全链路追踪编号")
    private String traceId;
}