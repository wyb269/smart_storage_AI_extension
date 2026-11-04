package com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 算法任务对外响应（详情弹窗「算法任务」表与轮询查询共用）。
 *
 * <p>字段按契约 AI_VISION_ALGORITHM_DATA_INTERACTION.md §1：
 * {@code taskNo / executionStatus / conclusion / confidence / modelName /
 * modelVersion / algorithmVersion / inferenceDurationMs / startedAt /
 * finishedAt / detectedCount / simulated / errorMessage}，
 * 另补本功能结论列 {@code occlusionType} 与错误码。</p>
 *
 * <p><b>安全</b>：不回传 {@code rawResultRef} 与 {@code detections} 原文，
 * 前端永不解析原始算法输出（AI_HANDOFF 第 24.3 节）；
 * {@code detectedCount} 由 SQL 侧从检测框 JSON 统计得出。</p>
 *
 * @author aiVision 功能族 · 视频遮挡检测
 */
@Data
@Schema(description = "视频遮挡算法任务响应")
public class OcclusionAlgorithmTaskRespDTO {

    @Schema(description = "任务主键（字符串形态，避免前端 Long 精度丢失）")
    private String taskId;

    @Schema(description = "算法任务编号，rerun/algorithmTask 端点的路径标识")
    private String taskNo;

    @Schema(description = "关联遮挡事件标识，可为空（未落事件的任务）")
    private String eventId;

    @Schema(description = "执行状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK")
    private String executionStatus;

    @Schema(description = "算法结论 NORMAL/ANOMALY/UNCERTAIN，未产出时为空")
    private String conclusion;

    @Schema(description = "标准化遮挡类型 FULL/PARTIAL/LENS_COVER/PAINT/OTHER")
    private String occlusionType;

    @Schema(description = "置信度 0-1，null 表示暂无，不得渲染为 0%")
    private BigDecimal confidence;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型版本")
    private String modelVersion;

    @Schema(description = "推理程序版本")
    private String algorithmVersion;

    @Schema(description = "算法运行配置版本")
    private String configVersion;

    @Schema(description = "推理耗时（毫秒）")
    private Long inferenceDurationMs;

    @Schema(description = "开始时间 yyyy-MM-dd HH:mm:ss")
    private String startedAt;

    @Schema(description = "完成时间 yyyy-MM-dd HH:mm:ss，进行中/失败/超时可为空")
    private String finishedAt;

    @Schema(description = "检出目标框数量")
    private Integer detectedCount;

    @Schema(description = "是否模拟结果；当前实现恒为 true（第 24.4 节）")
    private Boolean simulated;

    @Schema(description = "失败诊断码")
    private String errorCode;

    @Schema(description = "失败诊断信息（已脱敏），失败/超时态必须展示不许吞掉")
    private String errorMessage;

    @Schema(description = "重试次数")
    private Integer retryCount;
}
