package com.chinaunicom.smartstorage.admin.dto.doorAnomaly;

import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.algorithm.AlgorithmTaskRespDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 事件详情对象。
 *
 * <p>在 AI_HANDOFF.md 第 20.3 节列表字段基础上，
 * 增加第 20.4 节要求的详情字段：描述、算法任务编号、
 * 模型与算法版本、配置版本、推理时间、证据列表与处理记录列表。</p>
 *
 * @author 仓门异动功能
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "事件详情")
public class EventDetailRespDTO extends AnomalyEventRespDTO {

    @Schema(description = "事件描述")
    private String eventDesc;

    @Schema(description = "算法任务编号")
    private String algorithmTaskNo;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型版本")
    private String modelVersion;

    @Schema(description = "算法程序版本")
    private String algorithmVersion;

    @Schema(description = "配置版本")
    private String configVersion;

    @Schema(description = "推理开始时间")
    private String inferenceStartedAt;

    @Schema(description = "推理结束时间")
    private String inferenceFinishedAt;

    @Schema(description = "本次推理耗时（毫秒）")
    private Long inferenceDurationMs;

    /**
     * 是否为模拟数据。
     * <p>依据第 39.6 节，模拟数据必须带明确标识，
     * 不得与真实识别结果混淆，也不得混入真实统计。</p>
     */
    @Schema(description = "是否为模拟数据")
    private Boolean isSimulated;

    @Schema(description = "证据列表（一期至少含正常参考图与异常证据图）")
    private List<EventEvidenceRespDTO> evidences;

    @Schema(description = "处理记录列表（追加式，含历史全部记录）")
    private List<ProcessRecordRespDTO> processRecords;

    /**
     * 算法任务历史（契约 §1）：最多 20 条，按 started_at 倒序。
     * <p>给详情弹窗的「算法任务」表用，随详情一次性返回，前端不再单独请求。</p>
     */
    @Schema(description = "算法任务历史（最多 20 条，倒序）")
    private List<AlgorithmTaskRespDTO> algorithmTasks;

    /** 事件当前关联的算法任务（可空），给算法摘要行用 */
    @Schema(description = "最新算法任务（可空）")
    private AlgorithmTaskRespDTO latestAlgorithmTask;
}
