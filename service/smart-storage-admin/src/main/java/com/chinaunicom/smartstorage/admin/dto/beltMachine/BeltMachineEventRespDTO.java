package com.chinaunicom.smartstorage.admin.dto.beltMachine;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 皮带机下站人事件列表项。
 *
 * <p>对应原型图底部「闲人历史记录」表格的一行：
 * 事件编号 / 皮带机 / 摄像头 / 处理状态 / 操作。</p>
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
@Data
@Schema(description = "皮带机下站人事件列表项")
public class BeltMachineEventRespDTO {

    @Schema(description = "事件标识")
    private String eventId;

    @Schema(description = "事件编号，前缀 BC")
    private String eventNo;

    @Schema(description = "发生时间")
    private String occurredAt;

    @Schema(description = "仓库标识")
    private String warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "监测点位标识")
    private String pointId;

    @Schema(description = "皮带机业务编码")
    private String deviceCode;

    @Schema(description = "皮带机名称快照，列表「皮带机」列")
    private String deviceName;

    @Schema(description = "摄像头业务编码快照，列表「摄像头」列")
    private String cameraCode;

    @Schema(description = "结论类型编码")
    private String conclusionType;

    @Schema(description = "结论类型名称")
    private String conclusionTypeName;

    @Schema(description = "画面内检出人数，null 表示算法未给出")
    private Integer personCount;

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
     * 导出面向业务人员，需要直接可读的中文文案。</p>
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
