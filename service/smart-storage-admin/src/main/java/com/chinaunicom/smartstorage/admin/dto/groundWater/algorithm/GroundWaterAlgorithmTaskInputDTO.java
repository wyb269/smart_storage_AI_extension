package com.chinaunicom.smartstorage.admin.dto.groundWater.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 地面积水统一算法输入。
 *
 * <p>对齐 AI_HANDOFF.md 第 24.2 节「统一算法输入」，
 * 主体维度由遮挡功能的「摄像头」换为本功能的「监测点位」。</p>
 *
 * <p><b>安全约束（第 24.2、29.3 节）</b>：</p>
 * <ul>
 *   <li>{@code mediaRefs} 与 {@code normalReferenceRef} 只能存放<b>受控内部引用</b>，
 *       禁止存放任意 URL 或本地绝对路径，防止 SSRF 与路径穿越。</li>
 *   <li>算法服务只允许访问受控媒体引用，不得任意抓取外部地址。</li>
 * </ul>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
@Data
@Schema(description = "地面积水统一算法输入")
public class GroundWaterAlgorithmTaskInputDTO {

    @Schema(description = "算法任务唯一编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskId;

    @Schema(description = "积水事件编号")
    private String eventId;

    @Schema(description = "任务类型，地面积水识别固定 GROUND_WATER_DETECT",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskType;

    @Schema(description = "仓库标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String warehouseId;

    @Schema(description = "监测点位标识，指向 ai_water_monitor_point.id",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String pointId;

    @Schema(description = "设备侧摄像头标识")
    private String cameraId;

    @Schema(description = "摄像头业务编码，如 CAM-001")
    private String cameraCode;

    @Schema(description = "媒体类型 SINGLE_IMAGE/DUAL_IMAGE/FRAME_SEQUENCE/VIDEO")
    private String mediaType;

    @Schema(description = "受控内部媒体引用（对象存储键列表，禁止任意URL）")
    private List<String> mediaRefs;

    @Schema(description = "采集时间")
    private String capturedAt;

    @Schema(description = "可选干燥参考资源引用")
    private String normalReferenceRef;

    @Schema(description = "可选积水检测区域")
    private String roi;

    @Schema(description = "本次置信度阈值")
    private BigDecimal confidenceThreshold;

    @Schema(description = "配置版本")
    private String configVersion;

    @Schema(description = "全链路追踪编号")
    private String traceId;
}
