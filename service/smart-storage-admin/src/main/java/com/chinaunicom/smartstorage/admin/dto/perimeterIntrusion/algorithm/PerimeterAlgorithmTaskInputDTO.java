package com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 周界入侵统一算法输入。
 *
 * <p>对齐 AI_HANDOFF.md 第 24.2 节「统一算法输入」，
 * 主体维度由遮挡功能的「摄像头」换成本功能的「防区段」。</p>
 *
 * <p><b>安全约束（第 24.2、29.3 节）</b>：</p>
 * <ul>
 *   <li>{@code mediaRefs} 与 {@code normalReferenceRef} 只能存放<b>受控内部引用</b>，
 *       禁止存放任意 URL 或本地绝对路径，防止 SSRF 与路径穿越。</li>
 *   <li>算法服务只允许访问受控媒体引用，不得任意抓取外部地址。</li>
 * </ul>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
@Data
@Schema(description = "周界入侵统一算法输入")
public class PerimeterAlgorithmTaskInputDTO {

    @Schema(description = "算法任务唯一编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskId;

    @Schema(description = "入侵事件编号")
    private String eventId;

    @Schema(description = "任务类型，周界入侵识别固定 PERIMETER_INTRUSION_DETECT",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskType;

    @Schema(description = "仓库标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String warehouseId;

    @Schema(description = "防区段标识，指向 ai_perimeter_monitor_point.id",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String pointId;

    @Schema(description = "所属防区 EAST/WEST/SOUTH/NORTH")
    private String zone;

    @Schema(description = "绑定的摄像头配置标识")
    private String cameraConfigId;

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

    @Schema(description = "可选正常参考资源引用")
    private String normalReferenceRef;

    @Schema(description = "可选周界检测区域")
    private String roi;

    @Schema(description = "本次置信度阈值")
    private BigDecimal confidenceThreshold;

    @Schema(description = "布防状态 ARMED/DISARMED；撤防时算法应直接返回 SKIPPED")
    private String defenseStatus;

    @Schema(description = "配置版本")
    private String configVersion;

    @Schema(description = "全链路追踪编号")
    private String traceId;
}
