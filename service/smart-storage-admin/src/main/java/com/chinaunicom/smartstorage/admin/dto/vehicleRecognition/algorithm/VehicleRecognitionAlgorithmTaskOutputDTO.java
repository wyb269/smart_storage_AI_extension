package com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 车辆识别统一算法输出。
 *
 * <p>对齐算法适配层「统一算法输出」，遮挡侧的 {@code occlusionType} 在本功能为
 * 标准化车辆类型 {@code vehicleType}，取值同 {@code ai_vehicle_event.vehicle_type}。</p>
 *
 * <p><b>关键约束</b>：前端永远不直接解析 {@code rawResultRef}。
 * 真实算法包到达后，只需新增适配器把原始输出转换成本结构，
 * 不应修改页面接口与业务事件模型。</p>
 *
 * <p><b>PII 提示</b>：{@code plateNo} 是识别结论的一部分，属于个人轨迹信息，
 * 严禁出现在算法适配器的日志里（详见 Mock 适配器）。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Data
@Schema(description = "车辆识别统一算法输出")
public class VehicleRecognitionAlgorithmTaskOutputDTO {

    @Schema(description = "对应算法任务", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskId;

    @Schema(description = "执行状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String executionStatus;

    @Schema(description = "结论 RECOGNIZED/NO_VEHICLE/UNCERTAIN")
    private String conclusion;

    @Schema(description = "标准化车辆类型，取值同 ai_vehicle_event.vehicle_type")
    private String vehicleType;

    @Schema(description = "识别到的车牌号（PII，日志严禁打印完整值）")
    private String plateNo;

    @Schema(description = "行驶方向 IN/OUT")
    private String direction;

    @Schema(description = "置信度 0-1，可为空")
    private BigDecimal confidence;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型版本")
    private String modelVersion;

    @Schema(description = "推理程序版本")
    private String algorithmVersion;

    @Schema(description = "配置版本")
    private String configVersion;

    @Schema(description = "开始时间")
    private String startedAt;

    @Schema(description = "完成时间")
    private String finishedAt;

    @Schema(description = "推理耗时（毫秒）")
    private Long inferenceDurationMs;

    @Schema(description = "检测对象、区域和置信度")
    private List<DetectionDTO> detections;

    @Schema(description = "证据引用（车辆全景图、车牌特写等）")
    private List<EvidenceRefDTO> evidenceRefs;

    @Schema(description = "原始算法输出的受控引用（前端不解析）")
    private String rawResultRef;

    @Schema(description = "失败诊断码")
    private String errorCode;

    @Schema(description = "失败诊断信息（对前端脱敏）")
    private String errorMessage;

    /**
     * 是否为模拟结果。
     * <p>模拟结果必须明确标记，不得混入真实生产统计。含失败与超时分支。</p>
     */
    @Schema(description = "是否为模拟结果")
    private Boolean simulated = Boolean.FALSE;

    /**
     * 检测对象。
     */
    @Data
    @Schema(description = "算法检测对象")
    public static class DetectionDTO {

        @Schema(description = "检测类别")
        private String category;

        @Schema(description = "置信度 0-1")
        private BigDecimal confidence;

        @Schema(description = "检测框，JSON 数组如 [x1,y1,x2,y2]")
        private String bbox;

        @Schema(description = "跟踪标识")
        private String trackId;
    }

    /**
     * 证据引用。
     */
    @Data
    @Schema(description = "算法证据引用")
    public static class EvidenceRefDTO {

        @Schema(description = "证据类型 VEHICLE_IMAGE/PLATE_CROP/ANNOTATED 等")
        private String evidenceType;

        @Schema(description = "受控对象存储键，非完整 URL")
        private String objectKey;

        @Schema(description = "采集时间")
        private String capturedAt;
    }
}
