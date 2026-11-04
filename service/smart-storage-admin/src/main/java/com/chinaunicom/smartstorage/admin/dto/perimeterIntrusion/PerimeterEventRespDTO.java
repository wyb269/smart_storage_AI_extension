package com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 周界入侵事件列表项。
 *
 * <p>对应原型图底部「入侵事件记录」表格的一行：
 * 事件编号 / 防区 / 摄像头 / 处理状态 / 操作。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
@Data
@Schema(description = "周界入侵事件列表项")
public class PerimeterEventRespDTO {

    @Schema(description = "事件标识")
    private String eventId;

    @Schema(description = "事件编号，前缀 PD")
    private String eventNo;

    @Schema(description = "发生时间")
    private String occurredAt;

    @Schema(description = "仓库标识")
    private String warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "防区段标识")
    private String pointId;

    @Schema(description = "防区段名称，如 东防区-东墙段01")
    private String pointName;

    @Schema(description = "所属防区 EAST/WEST/SOUTH/NORTH")
    private String zone;

    @Schema(description = "防区展示名，页面「防区」列")
    private String zoneName;

    @Schema(description = "绑定摄像头配置标识")
    private String cameraConfigId;

    @Schema(description = "摄像头编号")
    private String cameraCode;

    @Schema(description = "摄像头安装位置快照")
    private String cameraPosition;

    @Schema(description = "入侵类型编码")
    private String intrusionType;

    @Schema(description = "入侵类型名称")
    private String intrusionTypeName;

    @Schema(description = "算法任务状态")
    private String algorithmStatus;

    @Schema(description = "算法结论")
    private String algorithmResult;

    @Schema(description = "置信度 0-1，null 表示暂无，不得渲染为 0%")
    private BigDecimal confidence;

    @Schema(description = "人工处理状态原始编码 PENDING/PROCESSING/DONE")
    private String processStatus;

    /**
     * 处理状态展示名（未核实 / 处理中 / 已处理）。
     *
     * <p><b>只在导出路径填充</b>：接口仍回原始编码由前端映射，
     * 导出面向业务人员，需要直接可读的文案。</p>
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
