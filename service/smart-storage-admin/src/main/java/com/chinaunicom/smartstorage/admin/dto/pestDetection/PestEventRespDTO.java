package com.chinaunicom.smartstorage.admin.dto.pestDetection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 虫害检测记录列表项。
 *
 * <p>对应原型图底部「虫害检测记录」表格的一行：
 * 采样编号 / 仓库 / 采样位置 / 虫情状态 / 操作。</p>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
@Data
@Schema(description = "虫害检测记录列表项")
public class PestEventRespDTO {

    @Schema(description = "事件标识")
    private String eventId;

    @Schema(description = "采样编号，前缀 PT")
    private String eventNo;

    @Schema(description = "采样时间")
    private String occurredAt;

    @Schema(description = "仓库标识")
    private String warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "粮面点位标识")
    private String pointId;

    @Schema(description = "摄像头编号")
    private String cameraCode;

    @Schema(description = "采样位置，如 东墙地上层")
    private String samplePosition;

    @Schema(description = "粮种")
    private String grainType;

    @Schema(description = "检测结论类型编码")
    private String conclusionType;

    @Schema(description = "检测结论类型名称")
    private String conclusionTypeName;

    @Schema(description = "虫种，无虫时为空")
    private String insectSpecies;

    @Schema(description = "密度（头/kg）")
    private BigDecimal density;

    @Schema(description = "判定阈值（头/kg）")
    private BigDecimal densityThreshold;

    @Schema(description = "算法任务状态")
    private String algorithmStatus;

    @Schema(description = "算法结论")
    private String algorithmResult;

    @Schema(description = "置信度 0-1，null 表示暂无，不得渲染为 0%")
    private BigDecimal confidence;

    @Schema(description = "人工处理状态原始编码 PENDING/PROCESSING/DONE，前端映射为三态展示")
    private String processStatus;

    /**
     * 处理状态展示名（未核实 / 处理中 / 已处理）。
     *
     * <p><b>只在导出路径填充</b>：接口仍回原始编码由前端映射，
     * 导出面向业务人员，需要直接可读的三态文案。</p>
     */
    @Schema(description = "处理状态展示名")
    private String processStatusName;

    @Schema(description = "最近处理人")
    private String processedBy;

    @Schema(description = "最近处理时间")
    private String processedAt;

    @Schema(description = "最近处理意见摘要")
    private String processOpinion;

    @Schema(description = "乐观锁版本号，提交处理时必须原样回传")
    private Integer eventVersion;
}
