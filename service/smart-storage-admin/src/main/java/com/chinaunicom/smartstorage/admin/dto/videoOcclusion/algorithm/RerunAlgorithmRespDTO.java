package com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 「重新识别」提交响应（契约 §1：提交 → 立即返回 taskId → 前端轮询查询）。
 *
 * <p>刻意只带任务受理信息，不带任何结论字段：算法不得在页面请求里
 * 同步长等（AI_HANDOFF 第 24.1 节），进度由 {@code algorithmTask/{taskId}} 查询。</p>
 *
 * @author aiVision 功能族 · 视频遮挡检测
 */
@Data
@Schema(description = "重新识别任务受理结果")
public class RerunAlgorithmRespDTO {

    @Schema(description = "算法任务编号，前端用它轮询 algorithmTask/{taskId}")
    private String taskId;

    @Schema(description = "初始执行状态，固定 QUEUED（真实受理态以轮询为准）")
    private String executionStatus;

    @Schema(description = "是否模拟结果；当前实现恒为 true，页面必须显示「模拟」标识")
    private Boolean simulated;
}
