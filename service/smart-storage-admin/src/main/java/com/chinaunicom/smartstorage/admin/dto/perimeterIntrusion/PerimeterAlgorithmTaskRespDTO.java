package com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 周界入侵算法任务响应。
 *
 * <p>供详情弹窗「算法任务」区与「重新识别」轮询共用，字段对齐
 * AI_VISION_ALGORITHM_DATA_INTERACTION.md §1 的最小字段集。
 * 时间一律 {@code yyyy-MM-dd HH:mm:ss} 字符串，与事件列表口径一致。</p>
 *
 * <p><b>taskNo 即对外 taskId</b>：重跑返回的 {@code rerunAlgorithm} 里的
 * {@code taskId} 就是本字段，前端凭它轮询 {@code algorithmTask/{taskId}}。
 * 数据库行主键 id 不外泄。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
@Data
@Schema(description = "周界入侵算法任务")
public class PerimeterAlgorithmTaskRespDTO {

    @Schema(description = "算法任务编号（对外 taskId）")
    private String taskId;

    @Schema(description = "算法任务编号，与 taskId 同值")
    private String taskNo;

    @Schema(description = "关联入侵事件标识")
    private String eventId;

    @Schema(description = "任务类型，固定 PERIMETER_INTRUSION_DETECT")
    private String taskType;

    @Schema(description = "执行状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK")
    private String executionStatus;

    @Schema(description = "算法结论 NORMAL/ANOMALY/UNCERTAIN；未产出为 null")
    private String conclusion;

    @Schema(description = "标准化入侵类型 CLIMB/CROSS/INTRUSION/LOITER/OTHER")
    private String intrusionType;

    @Schema(description = "置信度 0-1，null 表示暂无，不得渲染为 0%")
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

    @Schema(description = "完成时间；进行中/失败/超时为 null")
    private String finishedAt;

    @Schema(description = "检出目标数")
    private Integer detectedCount;

    @Schema(description = "是否模拟结果；页面必须据此显示「模拟」标识")
    private Boolean simulated;

    @Schema(description = "失败诊断码")
    private String errorCode;

    @Schema(description = "失败诊断信息（已脱敏），失败/超时态必须原样展示")
    private String errorMessage;
}
