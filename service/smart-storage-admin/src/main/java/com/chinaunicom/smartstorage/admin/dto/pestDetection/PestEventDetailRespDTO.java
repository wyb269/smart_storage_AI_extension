package com.chinaunicom.smartstorage.admin.dto.pestDetection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 虫害检测记录详情。
 *
 * <p>在列表字段之上补事件描述、密度是否超标与证据、处理记录，
 * 用于回答「为什么判为虫情告警、有什么证据」。</p>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "虫害检测记录详情")
public class PestEventDetailRespDTO extends PestEventRespDTO {

    @Schema(description = "事件描述")
    private String eventDesc;

    @Schema(description = "恢复/复检时间")
    private String recoveredAt;

    @Schema(description = "持续时长（秒）")
    private Long durationSeconds;

    @Schema(description = "是否模拟数据")
    private Boolean isSimulated;

    @Schema(description = "密度是否超过判定阈值（density > densityThreshold），派生展示用")
    private Boolean densityExceeded;

    @Schema(description = "证据列表，地址为短时签名")
    private List<PestEvidenceRespDTO> evidences;

    @Schema(description = "处理记录，追加式全量历史")
    private List<ProcessRecordRespDTO> processRecords;

    /**
     * 历史算法任务，按 started_at 倒序，最多 {@code MAX_ALGORITHM_TASKS_PER_EVENT} 条。
     * 详情弹窗「算法任务」表直接消费本字段，不再单独发请求。
     */
    @Schema(description = "算法任务历史（按开始时间倒序，最多 20 条）")
    private List<PestAlgorithmTaskRespDTO> algorithmTasks;

    /** 最新一条算法任务；无任何任务时为空 */
    @Schema(description = "最新算法任务（可为空）")
    private PestAlgorithmTaskRespDTO latestAlgorithmTask;
}
