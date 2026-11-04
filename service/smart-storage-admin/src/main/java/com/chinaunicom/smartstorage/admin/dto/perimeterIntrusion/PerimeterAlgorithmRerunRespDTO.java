package com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 「重新识别」受理响应。
 *
 * <p>依据契约 §1 与 AI_HANDOFF 第 24.1 节：算法不得在页面请求里同步长等，
 * 提交后<b>立即</b>返回任务标识与初始状态，由前端轮询
 * {@code algorithmTask/{taskId}} 获取进度。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
@Data
@Schema(description = "重新识别受理结果")
public class PerimeterAlgorithmRerunRespDTO {

    @Schema(description = "算法任务编号，前端凭它轮询")
    private String taskId;

    @Schema(description = "初始执行状态，恒为 QUEUED")
    private String executionStatus;

    @Schema(description = "是否模拟结果；本期 Mock 适配器恒为 true")
    private Boolean simulated;
}
