package com.chinaunicom.smartstorage.admin.dto.videoOcclusion;

import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm.OcclusionAlgorithmTaskRespDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 遮挡事件详情。
 *
 * <p>在列表字段之上补事件描述、遮挡持续时长与证据、处理记录，
 * 用于回答「为什么判为遮挡、有什么证据」。</p>
 *
 * <p>契约 §1 扩展：{@code algorithmTasks}（最多 20 条，按 started_at 倒序）
 * 与 {@code latestAlgorithmTask}（可空）供详情弹窗「算法任务」区一次拉齐，
 * 前端不再为任务表单独发请求。</p>
 *
 * @author aiVision 功能族 · 视频遮挡检测
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "遮挡事件详情")
public class OcclusionEventDetailRespDTO extends OcclusionEventRespDTO {

    @Schema(description = "事件描述")
    private String eventDesc;

    @Schema(description = "恢复时间")
    private String recoveredAt;

    @Schema(description = "遮挡持续时长（秒）")
    private Long durationSeconds;

    @Schema(description = "是否模拟数据")
    private Boolean isSimulated;

    @Schema(description = "证据列表，地址为短时签名")
    private List<OcclusionEvidenceRespDTO> evidences;

    @Schema(description = "处理记录，追加式全量历史")
    private List<ProcessRecordRespDTO> processRecords;

    @Schema(description = "算法任务列表，按 started_at 倒序，最多 20 条")
    private List<OcclusionAlgorithmTaskRespDTO> algorithmTasks;

    @Schema(description = "最近一次算法任务，可能为空（该事件从未跑过算法）")
    private OcclusionAlgorithmTaskRespDTO latestAlgorithmTask;
}
