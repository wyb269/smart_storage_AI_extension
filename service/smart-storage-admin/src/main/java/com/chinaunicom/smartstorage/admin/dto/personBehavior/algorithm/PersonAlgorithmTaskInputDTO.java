package com.chinaunicom.smartstorage.admin.dto.personBehavior.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 人员行为统一算法输入。
 *
 * <p>主体维度是<b>监测区域</b>：{@code pointId} 指向
 * {@code ai_person_monitor_point.id}，并保留区域内绑定摄像头的
 * {@code cameraId}、{@code cameraCode} 两个标识。</p>
 *
 * <p><b>安全约束</b>：{@code mediaRefs} 与 {@code normalReferenceRef}
 * 只能存放<b>受控内部引用</b>，禁止存放任意 URL 或本地绝对路径，
 * 防止 SSRF 与路径穿越。算法服务只允许访问受控媒体引用。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
@Data
@Schema(description = "人员行为统一算法输入")
public class PersonAlgorithmTaskInputDTO {

    @Schema(description = "算法任务唯一编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskId;

    @Schema(description = "人员行为事件编号")
    private String eventId;

    @Schema(description = "任务类型，人员行为识别固定 PERSON_BEHAVIOR_DETECT",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskType;

    @Schema(description = "仓库标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String warehouseId;

    @Schema(description = "监测区域标识，指向 ai_person_monitor_point.id",
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

    @Schema(description = "可选正常参考资源引用")
    private String normalReferenceRef;

    @Schema(description = "可选人员行为检测区域")
    private String roi;

    @Schema(description = "本次置信度阈值")
    private BigDecimal confidenceThreshold;

    @Schema(description = "人员聚集判定阈值（人数），达到即判 CROWD")
    private Integer crowdThreshold;

    @Schema(description = "配置版本")
    private String configVersion;

    @Schema(description = "全链路追踪编号")
    private String traceId;
}
