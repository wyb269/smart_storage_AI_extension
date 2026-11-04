package com.chinaunicom.smartstorage.admin.dto.doorAnomaly.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 算法任务对外视图对象。
 *
 * <p>契约 AI_VISION_ALGORITHM_DATA_INTERACTION.md §1：详情弹窗的「算法任务」区、
 * {@code listAlgorithmTasks} 与 {@code algorithmTask/{taskId}} 三处共用此结构，
 * 字段覆盖 §1 要求的最小集。</p>
 *
 * <p>字段命名对齐 {@code ai_algorithm_task} 列；时间统一输出
 * {@code yyyy-MM-dd HH:mm:ss} 字符串，与既有接口同形。</p>
 *
 * <p><b>安全约束</b>：不暴露 rawResultRef/detections 原文，仅给出
 * {@code detectedCount} 目标框数量，前端不解析算法原始输出（第 24.3 节）。</p>
 *
 * @author 仓门异动功能
 */
@Data
@Schema(description = "算法任务视图")
public class AlgorithmTaskRespDTO {

    @Schema(description = "算法任务主键（字符串形式）")
    private String taskId;

    @Schema(description = "算法任务编号")
    private String taskNo;

    @Schema(description = "执行状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK")
    private String executionStatus;

    @Schema(description = "算法结论 NORMAL/ANOMALY/UNCERTAIN，未产出时为空")
    private String conclusion;

    @Schema(description = "标准化异动类型")
    private String anomalyType;

    @Schema(description = "置信度 0-1，可为空")
    private BigDecimal confidence;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型版本")
    private String modelVersion;

    @Schema(description = "算法程序版本")
    private String algorithmVersion;

    @Schema(description = "配置版本")
    private String configVersion;

    @Schema(description = "推理耗时（毫秒）")
    private Long inferenceDurationMs;

    @Schema(description = "开始时间 yyyy-MM-dd HH:mm:ss")
    private String startedAt;

    @Schema(description = "结束时间 yyyy-MM-dd HH:mm:ss")
    private String finishedAt;

    @Schema(description = "检出目标框数量")
    private Integer detectedCount;

    @Schema(description = "是否为模拟结果（第 24.4 节，必须为 true）")
    private Boolean simulated;

    @Schema(description = "失败诊断码")
    private String errorCode;

    @Schema(description = "失败诊断信息（脱敏）")
    private String errorMessage;
}
