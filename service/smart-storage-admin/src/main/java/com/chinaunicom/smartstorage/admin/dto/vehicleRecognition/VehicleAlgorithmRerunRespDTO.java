package com.chinaunicom.smartstorage.admin.dto.vehicleRecognition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 「重新识别」提交结果。
 *
 * <p>算法不得在页面请求里同步长等：本接口只做「受理 + 落一行 QUEUED 任务」，
 * 立即返回任务标识，进度由前端轮询 {@code algorithmTask/{taskId}} 获取。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Data
@Schema(description = "重新识别提交受理结果")
public class VehicleAlgorithmRerunRespDTO {

    @Schema(description = "算法任务标识，用于轮询 algorithmTask/{taskId}")
    private String taskId;

    @Schema(description = "受理时的执行状态，固定 QUEUED")
    private String executionStatus;

    @Schema(description = "是否模拟结果；本适配器产出的一律为 true")
    private Boolean simulated;
}
