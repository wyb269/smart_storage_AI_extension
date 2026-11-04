package com.chinaunicom.smartstorage.admin.dto.groundWater.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交算法重跑任务的受理回执。
 *
 * <p>契约 §1 / AI_HANDOFF 第 24.1 节：算法不许在页面请求里同步长等，
 * 提交端点<b>只做受理</b>——落一行 QUEUED 任务并立即返回任务编号，
 * 进度由前端轮询 {@code algorithmTask/{taskId}} 获取。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "算法重跑受理回执")
public class RerunAlgorithmRespDTO {

    @Schema(description = "算法任务编号（轮询句柄，同 task_no）")
    private String taskId;

    @Schema(description = "受理时的执行状态，恒为 QUEUED")
    private String executionStatus;

    @Schema(description = "是否模拟结果；当前唯一实现是 Mock 适配器，恒为 true")
    private Boolean simulated;
}
