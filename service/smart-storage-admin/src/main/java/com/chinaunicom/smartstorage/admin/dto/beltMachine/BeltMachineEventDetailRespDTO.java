package com.chinaunicom.smartstorage.admin.dto.beltMachine;

import com.chinaunicom.smartstorage.admin.dto.beltMachine.algorithm.BeltMachineAlgorithmTaskRespDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 皮带机下站人事件详情。
 *
 * <p>在列表字段之上补事件描述、持续时长与证据、处理记录，
 * 用于回答「为什么判为站人、有什么证据」。</p>
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "皮带机下站人事件详情")
public class BeltMachineEventDetailRespDTO extends BeltMachineEventRespDTO {

    @Schema(description = "事件描述")
    private String eventDesc;

    @Schema(description = "恢复时间")
    private String recoveredAt;

    @Schema(description = "异常持续时长（秒）")
    private Long durationSeconds;

    @Schema(description = "是否模拟数据")
    private Boolean isSimulated;

    @Schema(description = "证据列表，地址为短时签名")
    private List<BeltMachineEvidenceRespDTO> evidences;

    @Schema(description = "处理记录，追加式全量历史")
    private List<BeltMachineProcessRecordRespDTO> processRecords;

    @Schema(description = "算法任务列表，按开始时间倒序，最多 20 条；详情弹窗「算法任务」区数据源")
    private List<BeltMachineAlgorithmTaskRespDTO> algorithmTasks;

    @Schema(description = "最近一次算法任务，可为空（历史事件补数前或任务表为空时为 null）")
    private BeltMachineAlgorithmTaskRespDTO latestAlgorithmTask;
}
