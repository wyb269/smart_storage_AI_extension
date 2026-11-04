package com.chinaunicom.smartstorage.admin.dto.groundWater.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 地面积水算法任务响应对象。
 *
 * <p>详情弹窗「算法任务」区与三个算法端点共用的返回形状。
 * 字段集合按《算法数据与重新识别交互契约》§1：
 * taskNo / executionStatus / conclusion / confidence / modelName / modelVersion /
 * algorithmVersion / configVersion / inferenceDurationMs / startedAt / finishedAt /
 * detectedCount / simulated / errorMessage，另补本功能自己的结论列 waterType。</p>
 *
 * <p><b>安全约束</b>：不回传 media_refs / raw_result_ref 等内部引用原文，
 * 页面只需要任务结论与运行元信息。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
@Data
@Schema(description = "地面积水算法任务")
public class WaterAlgorithmTaskRespDTO {

    @Schema(description = "任务主键（字符串形式）")
    private String taskId;

    @Schema(description = "算法任务编号，如 WATA202610280001 / MOCK-TASK-…")
    private String taskNo;

    @Schema(description = "关联积水事件标识")
    private String eventId;

    @Schema(description = "执行状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK")
    private String executionStatus;

    @Schema(description = "算法结论 NORMAL/ANOMALY/UNCERTAIN；未产出为 null")
    private String conclusion;

    @Schema(description = "积水类型 PONDING/LEAK/ICE/OTHER；未出结论为 null")
    private String waterType;

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

    @Schema(description = "开始时间 yyyy-MM-dd HH:mm:ss")
    private String startedAt;

    @Schema(description = "完成时间 yyyy-MM-dd HH:mm:ss；进行中/失败前为空")
    private String finishedAt;

    @Schema(description = "检出目标框数量")
    private Integer detectedCount;

    @Schema(description = "是否模拟结果；铁律：模拟必须为 true，页面必须显示标识")
    private Boolean simulated;

    @Schema(description = "失败诊断码")
    private String errorCode;

    @Schema(description = "失败诊断信息（对前端脱敏）")
    private String errorMessage;
}
