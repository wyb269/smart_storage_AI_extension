package com.chinaunicom.smartstorage.admin.dto.doorAnomaly.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 「重新识别」受理结果。
 *
 * <p>契约 AI_VISION_ALGORITHM_DATA_INTERACTION.md §1 的
 * {@code rerunAlgorithm/{eventId}} 返回体：{@code {taskId, executionStatus, simulated}}。
 * 算法不在请求里同步长等（第 24.1 节），这里只回执受理状态，
 * 由前端轮询 {@code algorithmTask/{taskId}} 获取进度。</p>
 *
 * @author 仓门异动功能
 */
@Data
@Schema(description = "重新识别受理结果")
public class AlgorithmRerunRespDTO {

    @Schema(description = "算法任务标识（即 taskNo，用于后续轮询）")
    private String taskId;

    @Schema(description = "受理时的执行状态，固定 QUEUED")
    private String executionStatus;

    @Schema(description = "是否为模拟结果（第 24.4 节，必须为 true）")
    private Boolean simulated;
}
