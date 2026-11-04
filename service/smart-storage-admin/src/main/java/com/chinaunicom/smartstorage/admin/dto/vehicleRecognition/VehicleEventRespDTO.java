package com.chinaunicom.smartstorage.admin.dto.vehicleRecognition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 车辆识别记录列表项。
 *
 * <p>对应原型图底部「出入事件记录」表格的一行：
 * 车牌号 / 车辆类型 / 行驶方向 / 识别地点 / 状态 / 操作。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Data
@Schema(description = "车辆识别记录列表项")
public class VehicleEventRespDTO {

    @Schema(description = "记录标识")
    private String eventId;

    @Schema(description = "事件编号，前缀 VC")
    private String eventNo;

    @Schema(description = "车牌号（PII，日志严禁打印完整值）")
    private String plateNo;

    @Schema(description = "车辆类型")
    private String vehicleType;

    @Schema(description = "车身颜色")
    private String vehicleColor;

    @Schema(description = "行驶方向编码 IN/OUT")
    private String direction;

    @Schema(description = "行驶方向名称 入库/出库")
    private String directionName;

    @Schema(description = "识别点位标识")
    private String pointId;

    @Schema(description = "识别地点（点位名快照）")
    private String gateName;

    @Schema(description = "识别时间")
    private String recognizedAt;

    @Schema(description = "仓库标识")
    private String warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "摄像头编号快照")
    private String cameraCode;

    @Schema(description = "算法任务状态")
    private String algorithmStatus;

    @Schema(description = "识别置信度 0-1，null 表示暂无，不得渲染为 0%")
    private BigDecimal confidence;

    @Schema(description = "核实状态原始编码，前端映射为展示文案")
    private String verifyStatus;

    /**
     * 核实状态展示名（待核实 / 已核实 / 误报）。
     *
     * <p><b>只在导出路径填充</b>：接口仍回原始编码由前端映射，
     * 导出面向业务人员，需要直接可读的文案。</p>
     */
    @Schema(description = "核实状态展示名")
    private String verifyStatusName;

    @Schema(description = "最近核实人")
    private String processedBy;

    @Schema(description = "最近核实时间")
    private String processedAt;

    @Schema(description = "最近核实意见摘要")
    private String verifyOpinion;

    @Schema(description = "乐观锁版本号，提交处理时必须原样回传")
    private Integer eventVersion;
}
