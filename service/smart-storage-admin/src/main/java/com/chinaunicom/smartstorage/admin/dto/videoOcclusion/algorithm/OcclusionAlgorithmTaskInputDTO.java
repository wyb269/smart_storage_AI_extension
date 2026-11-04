package com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 视频遮挡统一算法输入。
 *
 * <p>对齐 AI_HANDOFF.md 第 24.2 节「统一算法输入」，
 * 主体维度由仓门异动的「仓门」换成本功能的「摄像头」。</p>
 *
 * <p><b>安全约束（第 24.2、29.3 节）</b>：</p>
 * <ul>
 *   <li>{@code mediaRefs} 与 {@code normalReferenceRef} 只能存放<b>受控内部引用</b>，
 *       禁止存放任意 URL 或本地绝对路径，防止 SSRF 与路径穿越。</li>
 *   <li>算法服务只允许访问受控媒体引用，不得任意抓取外部地址。</li>
 * </ul>
 *
 * @author 视频遮挡检测功能
 */
@Data
@Schema(description = "视频遮挡统一算法输入")
public class OcclusionAlgorithmTaskInputDTO {

    @Schema(description = "算法任务唯一编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskId;

    @Schema(description = "遮挡事件编号")
    private String eventId;

    @Schema(description = "任务类型，视频遮挡识别固定 VIDEO_OCCLUSION_DETECT",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskType;

    @Schema(description = "仓库标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String warehouseId;

    @Schema(description = "摄像头配置标识，指向 ai_occlusion_camera.id",
            requiredMode = Schema.RequiredMode.REQUIRED)
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

    @Schema(description = "可选遮挡检测区域")
    private String roi;

    @Schema(description = "本次置信度阈值")
    private BigDecimal confidenceThreshold;

    @Schema(description = "配置版本")
    private String configVersion;

    @Schema(description = "全链路追踪编号")
    private String traceId;
}
