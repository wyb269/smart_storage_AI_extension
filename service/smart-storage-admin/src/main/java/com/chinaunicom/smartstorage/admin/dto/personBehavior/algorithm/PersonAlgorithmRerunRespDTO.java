package com.chinaunicom.smartstorage.admin.dto.personBehavior.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 「重新识别」受理响应。
 *
 * <p>算法不得在页面请求里同步长等：本接口只做受理与落行，
 * 立即返回任务编号，进度由 {@code algorithmTask/{taskId}} 轮询获取。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
@Data
@Schema(description = "人员行为重新识别受理结果")
public class PersonAlgorithmRerunRespDTO {

    @Schema(description = "算法任务编号（轮询句柄）")
    private String taskId;

    @Schema(description = "初始执行状态，固定 QUEUED")
    private String executionStatus;

    @Schema(description = "是否模拟结果，当前实现恒为 true")
    private Boolean simulated;
}
