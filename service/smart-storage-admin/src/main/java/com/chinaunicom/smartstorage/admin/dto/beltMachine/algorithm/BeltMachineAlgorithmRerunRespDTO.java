package com.chinaunicom.smartstorage.admin.dto.beltMachine.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 「重新识别」受理结果。
 *
 * <p>依据 AI_HANDOFF 第 24.1 节，算法不得在页面请求里同步长等：
 * 提交接口<b>只回执任务标识与初始状态</b>，进度由前端轮询
 * {@code /beltMachine/algorithmTask/{taskId}} 获取。</p>
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
@Data
@Schema(description = "皮带机重新识别受理结果")
public class BeltMachineAlgorithmRerunRespDTO {

    @Schema(description = "算法任务标识（即 task_no，轮询接口的路径参数）")
    private String taskId;

    @Schema(description = "初始执行状态，受理后为 QUEUED 或 RUNNING")
    private String executionStatus;

    @Schema(description = "是否模拟结果，当前适配器恒为 true")
    private Boolean simulated;
}
