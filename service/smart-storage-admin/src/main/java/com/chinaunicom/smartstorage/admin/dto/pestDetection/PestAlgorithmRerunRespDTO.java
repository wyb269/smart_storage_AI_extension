package com.chinaunicom.smartstorage.admin.dto.pestDetection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 「重新识别」提交结果。
 *
 * <p>算法不得在页面请求里同步长等：本对象只携带任务受理信息，
 * 结果由前端轮询 {@code algorithmTask/{taskId}} 获取。</p>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
@Data
@Schema(description = "重新识别提交受理结果")
public class PestAlgorithmRerunRespDTO {

    @Schema(description = "算法任务标识（即 task_no，轮询查询用）")
    private String taskId;

    @Schema(description = "初始执行状态，固定 QUEUED")
    private String executionStatus;

    @Schema(description = "是否模拟结果；当前实现恒为 true")
    private Boolean simulated;
}