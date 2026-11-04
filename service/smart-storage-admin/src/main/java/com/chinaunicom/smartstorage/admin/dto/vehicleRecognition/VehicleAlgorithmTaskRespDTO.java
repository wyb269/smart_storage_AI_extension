package com.chinaunicom.smartstorage.admin.dto.vehicleRecognition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 车辆识别算法任务响应。
 *
 * <p>数据来源 {@code ai_vehicle_algorithm_task}，供详情弹窗的「算法任务」区块、
 * {@code algorithmTask/{taskId}} 轮询与 {@code listAlgorithmTasks/{eventId}} 使用。</p>
 *
 * <p><b>PII 出口控制</b>：本 DTO <b>刻意不包含完整车牌</b>。车牌属个人轨迹信息，
 * 算法任务侧只回传掩码值（{@code 黑A***00}）与事件引用，
 * 完整车牌仅由识别记录详情接口按既有口径返回。新增字段前请先确认是否会把原文带出。</p>
 *
 * <p>{@code simulated} 为真时前端必须显示「模拟结果」标识，不得混入真实统计。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Data
@Schema(description = "车辆识别算法任务")
public class VehicleAlgorithmTaskRespDTO {

    @Schema(description = "算法任务标识（等于 task_no，轮询 algorithmTask/{taskId} 用它）")
    private String taskId;

    @Schema(description = "算法任务编号")
    private String taskNo;

    @Schema(description = "关联识别记录标识")
    private String eventId;

    @Schema(description = "识别点位标识（本功能算法主体）")
    private String pointId;

    @Schema(description = "任务类型，本功能固定 VEHICLE_RECOGNIZE")
    private String taskType;

    @Schema(description = "执行状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK")
    private String executionStatus;

    @Schema(description = "结论 RECOGNIZED/NO_VEHICLE/UNCERTAIN")
    private String conclusion;

    @Schema(description = "置信度 0-1；null 表示暂无，不得渲染为 0%")
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

    @Schema(description = "开始时间 yyyy-MM-dd HH:mm:ss")
    private String startedAt;

    @Schema(description = "完成时间 yyyy-MM-dd HH:mm:ss；进行中/失败/超时为空")
    private String finishedAt;

    @Schema(description = "检出目标数（车牌/车型/方向检测框合计）")
    private Integer detectedCount;

    @Schema(description = "是否模拟结果，模拟必须为 true 且页面显示「模拟结果」标识")
    private Boolean simulated;

    @Schema(description = "失败诊断码，如 PLATE_UNREADABLE")
    private String errorCode;

    @Schema(description = "失败诊断信息（不含车牌原文）")
    private String errorMessage;

    @Schema(description = "算法给出的标准化车型")
    private String vehicleType;

    @Schema(description = "算法给出的行驶方向 IN/OUT")
    private String direction;

    @Schema(description = "算法给出的车牌掩码（PII 脱敏后，绝不含完整车牌）")
    private String plateMasked;

    @Schema(description = "车牌掩码引用，如 event:7003；用于替代车牌原文做定位")
    private String plateRef;
}
