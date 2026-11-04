package com.chinaunicom.smartstorage.admin.dto.doorAnomaly;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 异动事件列表对象。
 *
 * <p>字段严格对齐 AI_HANDOFF.md 第 20.3 节「事件列表对象」。</p>
 *
 * <p><b>关键语义</b>：algorithmStatus（算法任务状态）、
 * algorithmResult（算法结论）、processStatus（人工处理状态）
 * 是三个相互独立的状态维度，任何一个都不得由另一个推导或覆盖。</p>
 *
 * @author 仓门异动功能
 */
@Data
@Schema(description = "异动事件")
public class AnomalyEventRespDTO {

    @Schema(description = "内部唯一标识")
    private String eventId;

    @Schema(description = "对外事件编号")
    private String eventNo;

    @Schema(description = "异动发生时间")
    private String occurredAt;

    @Schema(description = "仓库标识")
    private String warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "仓门标识")
    private String doorId;

    @Schema(description = "仓门名称")
    private String doorName;

    @Schema(description = "摄像头标识")
    private String cameraId;

    @Schema(description = "摄像头编号")
    private String cameraCode;

    @Schema(description = "异动类型编码")
    private String anomalyType;

    @Schema(description = "异动类型名称")
    private String anomalyTypeName;

    @Schema(description = "算法任务状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK")
    private String algorithmStatus;

    @Schema(description = "算法结论 NORMAL/ANOMALY/UNCERTAIN")
    private String algorithmResult;

    /**
     * 置信度，0 到 1，可为空。
     * <p>空表示「暂无」，与数值 0 含义不同，序列化时不得将 null 转为 0。</p>
     */
    @Schema(description = "置信度 0-1，可为空")
    private java.math.BigDecimal confidence;

    @Schema(description = "人工处理状态 PENDING/PROCESSING/VERIFIED_TRUE/VERIFIED_FALSE/CLOSED")
    private String processStatus;

    /**
     * 处理状态展示名，导出链路填充。
     * <p>产品口径只有两态：<b>未处理 / 已处理</b>。库内历史遗留的
     * PROCESSING、VERIFIED_TRUE、VERIFIED_FALSE 一律归入「已处理」，
     * 不得在导出表里再暴露五种状态。</p>
     */
    @Schema(description = "处理状态展示名（未处理/已处理）")
    private String processStatusName;

    @Schema(description = "最近处理人（后端按登录态记录，不信任前端传入）")
    private String processedBy;

    @Schema(description = "最近处理时间")
    private String processedAt;

    /**
     * 最近处理意见摘要。
     * <p>仅供列表与导出快速展示；<b>完整历史</b>在处理记录列表中，
     * 不得只覆盖最后一次处理意见（第 17.6 节）。</p>
     */
    @Schema(description = "最近处理意见摘要")
    private String processOpinion;

    @Schema(description = "并发控制版本号")
    private Integer eventVersion;
}
