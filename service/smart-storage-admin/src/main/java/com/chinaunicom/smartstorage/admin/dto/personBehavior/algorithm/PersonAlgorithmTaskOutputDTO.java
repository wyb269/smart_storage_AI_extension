package com.chinaunicom.smartstorage.admin.dto.personBehavior.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 人员行为统一算法输出。
 *
 * <p>遮挡侧的 {@code occlusionType} 在本功能为标准化行为类型
 * {@code behaviorType}，取值同 {@code ai_person_event.behavior_type}。</p>
 *
 * <p><b>关键约束</b>：前端永远不直接解析 {@code rawResultRef}。
 * 真实算法包到达后，只需新增适配器把原始输出转换成本结构，
 * 不应修改页面接口与业务事件模型。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
@Data
@Schema(description = "人员行为统一算法输出")
public class PersonAlgorithmTaskOutputDTO {

    @Schema(description = "对应算法任务", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taskId;

    @Schema(description = "执行状态 SUCCEEDED/FAILED/TIMEOUT",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String executionStatus;

    @Schema(description = "结论 NORMAL/ANOMALY/UNCERTAIN")
    private String conclusion;

    @Schema(description = "标准化行为类型 CROWD/NO_HELMET/FALL/INTRUSION/OTHER")
    private String behaviorType;

    @Schema(description = "置信度 0-1，可为空")
    private BigDecimal confidence;

    @Schema(description = "本次画面内检测到的人数，聚集判定的依据")
    private Integer personCount;

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

    @Schema(description = "证据引用（正常参考图、行为异常帧、恢复图等）")
    private List<EvidenceRefDTO> evidenceRefs;

    @Schema(description = "原始算法输出的受控引用（前端不解析）")
    private String rawResultRef;

    @Schema(description = "失败诊断码")
    private String errorCode;

    @Schema(description = "失败诊断信息（对前端脱敏）")
    private String errorMessage;

    /**
     * 是否为模拟结果。
     * <p>模拟结果必须明确标记，不得混入真实生产统计。含失败、超时与未产出分支。</p>
     */
    @Schema(description = "是否为模拟结果")
    private Boolean simulated = Boolean.FALSE;

    /**
     * 检测对象。
     */
    @Data
    @Schema(description = "算法检测对象")
    public static class DetectionDTO {

        @Schema(description = "检测类别，如 person / helmet / no_helmet")
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

        @Schema(description = "证据类型 NORMAL_REFERENCE/BEHAVIOR_FRAME/RECOVERY_IMAGE/EVENT_VIDEO 等")
        private String evidenceType;

        @Schema(description = "受控对象存储键，非完整 URL")
        private String objectKey;

        @Schema(description = "采集时间")
        private String capturedAt;
    }
}
