package com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 车辆识别统一算法输入。
 *
 * <p>对齐算法适配层「统一算法输入」，主体维度由遮挡的「摄像头配置」换成本功能的「识别点位」。</p>
 *
 * <p><b>安全约束</b>：</p>
 * <ul>
 *   <li>{@code mediaRefs} 与 {@code normalReferenceRef} 只能存放<b>受控内部引用</b>，
 *       禁止存放任意 URL 或本地绝对路径，防止 SSRF 与路径穿越。</li>
 *   <li>算法服务只允许访问受控媒体引用，不得任意抓取外部地址。</li>
 * </ul>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Data
@Schema(description = "车辆识别统一算法输入")
public class VehicleRecognitionAlgorithmTaskInputDTO {

    @Schema(description = "算法任务唯一编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskId;

    @Schema(description = "识别记录编号")
    private String eventId;

    @Schema(description = "任务类型，车辆识别固定 VEHICLE_RECOGNIZE",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskType;

    @Schema(description = "仓库标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String warehouseId;

    @Schema(description = "识别点位标识，指向 ai_vehicle_monitor_point.id",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String pointId;

    @Schema(description = "设备侧摄像头标识")
    private String cameraId;

    @Schema(description = "摄像头业务编码，如 CAM-003")
    private String cameraCode;

    @Schema(description = "媒体类型 SINGLE_IMAGE/DUAL_IMAGE/FRAME_SEQUENCE/VIDEO")
    private String mediaType;

    @Schema(description = "受控内部媒体引用（对象存储键列表，禁止任意URL）")
    private List<String> mediaRefs;

    @Schema(description = "采集时间")
    private String capturedAt;

    @Schema(description = "可选正常参考资源引用")
    private String normalReferenceRef;

    @Schema(description = "可选识别区域")
    private String roi;

    @Schema(description = "本次置信度阈值")
    private BigDecimal confidenceThreshold;

    @Schema(description = "配置版本")
    private String configVersion;

    @Schema(description = "全链路追踪编号")
    private String traceId;
}
