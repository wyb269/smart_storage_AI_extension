package com.chinaunicom.smartstorage.admin.dto.groundWater;

import com.chinaunicom.smartstorage.admin.dto.groundWater.algorithm.WaterAlgorithmTaskRespDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 积水事件详情。
 *
 * <p>在列表字段之上补事件描述、积水持续时长与证据、处理记录、算法任务，
 * 用于回答「为什么判为积水、有什么证据、算法是怎么跑的」。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "积水事件详情")
public class WaterEventDetailRespDTO extends WaterEventRespDTO {

    @Schema(description = "事件描述")
    private String eventDesc;

    @Schema(description = "恢复时间（水已排除）")
    private String recoveredAt;

    @Schema(description = "积水持续时长（秒）")
    private Long durationSeconds;

    @Schema(description = "是否模拟数据")
    private Boolean isSimulated;

    @Schema(description = "证据列表，地址为短时签名")
    private List<WaterEvidenceRespDTO> evidences;

    @Schema(description = "处理记录，追加式全量历史")
    private List<ProcessRecordRespDTO> processRecords;

    /**
     * 该事件的算法任务列表，按 started_at 倒序，最多 20 条（契约 §1）。
     *
     * <p>供详情弹窗「算法任务」表直接消费，前端不再单独发 listAlgorithmTasks 请求。</p>
     */
    @Schema(description = "算法任务列表（最多 20 条，按开始时间倒序）")
    private List<WaterAlgorithmTaskRespDTO> algorithmTasks;

    @Schema(description = "最近一条算法任务；事件从未跑过算法时为 null")
    private WaterAlgorithmTaskRespDTO latestAlgorithmTask;
}
