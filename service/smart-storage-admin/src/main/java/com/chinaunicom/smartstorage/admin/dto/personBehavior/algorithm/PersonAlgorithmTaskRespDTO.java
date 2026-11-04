package com.chinaunicom.smartstorage.admin.dto.personBehavior.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 算法任务对外响应（详情弹窗「算法任务」区与轮询接口共用）。
 *
 * <p>字段是 {@code ai_person_algorithm_task} 的展示投影：
 * 时间统一格式化为 {@code yyyy-MM-dd HH:mm:ss}，与列表接口同形；
 * {@code detectedCount} 由 detections JSON 的框数派生，不是表列。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
@Data
@Schema(description = "人员行为算法任务响应")
public class PersonAlgorithmTaskRespDTO {

    @Schema(description = "算法任务编号（前端轮询句柄，即 task_no）")
    private String taskId;

    @Schema(description = "算法任务编号")
    private String taskNo;

    @Schema(description = "关联事件标识")
    private String eventId;

    @Schema(description = "执行状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK")
    private String executionStatus;

    @Schema(description = "结论 NORMAL/ANOMALY/UNCERTAIN；未产出为空")
    private String conclusion;

    @Schema(description = "标准化行为类型 CROWD/NO_HELMET/FALL/INTRUSION/OTHER")
    private String behaviorType;

    @Schema(description = "置信度 0-1，null 表示暂无")
    private BigDecimal confidence;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型版本")
    private String modelVersion;

    @Schema(description = "推理程序版本")
    private String algorithmVersion;

    @Schema(description = "配置版本")
    private String configVersion;

    @Schema(description = "推理耗时（毫秒）")
    private Long inferenceDurationMs;

    @Schema(description = "开始时间")
    private String startedAt;

    @Schema(description = "完成时间；进行中与失败/超时为空")
    private String finishedAt;

    @Schema(description = "检出目标数（detections JSON 的框数）")
    private Integer detectedCount;

    @Schema(description = "是否模拟结果；模拟必须为 true 并在页面标识")
    private Boolean simulated;

    @Schema(description = "失败诊断码")
    private String errorCode;

    @Schema(description = "失败诊断信息（对前端脱敏）")
    private String errorMessage;
}
