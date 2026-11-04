package com.chinaunicom.smartstorage.admin.dto.beltMachine.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 皮带机下站人算法任务视图（详情弹窗「算法任务」区与轮询接口共用）。
 *
 * <p>对应表 {@code ai_belt_algorithm_task} 的一次执行记录。
 * 字段名按前后端契约输出驼峰，时间为 {@code yyyy-MM-dd HH:mm:ss} 字符串，
 * 与既有事件接口的时间格式一致。</p>
 *
 * <p><b>安全约束</b>：本视图<b>不携带</b> {@code mediaRefs}、{@code rawResultRef}
 * 等受控内部引用原文，前端永远拿不到对象存储键，
 * 避免越权拼地址（AI_HANDOFF 第 29 节）。</p>
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
@Data
@Schema(description = "皮带机下站人算法任务视图")
public class BeltMachineAlgorithmTaskRespDTO {

    @Schema(description = "算法任务库内标识（字符串化的 BIGINT）")
    private String taskId;

    @Schema(description = "算法任务编号，与网关任务标识同源")
    private String taskNo;

    @Schema(description = "执行状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK")
    private String executionStatus;

    @Schema(description = "算法结论 NORMAL/ANOMALY/UNCERTAIN，未产出为空")
    private String conclusion;

    @Schema(description = "标准化结论类型 STANDING_UNDER/CROSSING/LOITERING/OTHER")
    private String conclusionType;

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

    @Schema(description = "完成时间，进行中与失败超时为空")
    private String finishedAt;

    @Schema(description = "检出目标数，由 detections JSON 数组长度派生")
    private Integer detectedCount;

    @Schema(description = "是否模拟结果，模拟必须为 true 并在页面显示标识")
    private Boolean simulated;

    @Schema(description = "失败诊断码")
    private String errorCode;

    @Schema(description = "失败诊断信息，前端原样展示，不吞掉")
    private String errorMessage;
}
