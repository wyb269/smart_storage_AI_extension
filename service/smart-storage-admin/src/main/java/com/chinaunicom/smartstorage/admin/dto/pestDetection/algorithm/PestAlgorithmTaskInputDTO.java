package com.chinaunicom.smartstorage.admin.dto.pestDetection.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 粮面虫害统一算法输入。
 *
 * <p>对齐遮挡那版的「统一算法输入」，主体维度由摄像头换成本功能的「粮面点位」。</p>
 *
 * <p><b>安全约束</b>：{@code mediaRefs} 与 {@code normalReferenceRef} 只能存放
 * <b>受控内部引用</b>，禁止存放任意 URL 或本地绝对路径，防止 SSRF 与路径穿越。</p>
 *
 * @author 粮面虫害智能识别功能
 */
@Data
@Schema(description = "粮面虫害统一算法输入")
public class PestAlgorithmTaskInputDTO {

    @Schema(description = "算法任务唯一编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskId;

    @Schema(description = "检测事件编号")
    private String eventId;

    @Schema(description = "任务类型，粮面虫害识别固定 PEST_DETECT",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskType;

    @Schema(description = "仓库标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String warehouseId;

    @Schema(description = "粮面点位标识，指向 ai_pest_monitor_point.id",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String pointId;

    @Schema(description = "设备侧摄像头标识")
    private String cameraId;

    @Schema(description = "摄像头业务编码，如 CAM-003")
    private String cameraCode;

    @Schema(description = "采样位置，如 东墙地上层")
    private String samplePosition;

    @Schema(description = "媒体类型 SINGLE_IMAGE/DUAL_IMAGE/FRAME_SEQUENCE/VIDEO")
    private String mediaType;

    @Schema(description = "受控内部媒体引用（对象存储键列表，禁止任意URL）")
    private List<String> mediaRefs;

    @Schema(description = "采集时间")
    private String capturedAt;

    @Schema(description = "可选正常参考资源引用")
    private String normalReferenceRef;

    @Schema(description = "可选检测区域")
    private String roi;

    @Schema(description = "本次密度判定阈值（头/kg），缺省取服务端常量")
    private BigDecimal densityThreshold;

    @Schema(description = "本次置信度阈值")
    private BigDecimal confidenceThreshold;

    @Schema(description = "配置版本")
    private String configVersion;

    @Schema(description = "全链路追踪编号")
    private String traceId;
}
