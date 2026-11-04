package com.chinaunicom.smartstorage.admin.dto.vehicleRecognition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 车辆识别记录详情。
 *
 * <p>在列表字段之上补事件描述、模拟数据标识与证据、核实记录，
 * 用于回答「这条识别是怎么来的、有什么证据、谁核实的」。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "车辆识别记录详情")
public class VehicleEventDetailRespDTO extends VehicleEventRespDTO {

    @Schema(description = "事件描述")
    private String eventDesc;

    @Schema(description = "是否模拟数据")
    private Boolean isSimulated;

    @Schema(description = "证据列表，地址为短时签名")
    private List<VehicleEvidenceRespDTO> evidences;

    @Schema(description = "核实记录，追加式全量历史")
    private List<ProcessRecordRespDTO> processRecords;

    /**
     * 算法任务历史，按开始时间倒序，最多 {@code MAX_ALGORITHM_TASKS_IN_DETAIL} 条。
     *
     * <p>随详情一次性带回：详情弹窗的任务表不再单独发一次 listAlgorithmTasks 请求，
     * 避免弹窗打开时多一次往返与两套时序。</p>
     */
    @Schema(description = "算法任务历史（开始时间倒序，最多 20 条）")
    private List<VehicleAlgorithmTaskRespDTO> algorithmTasks;

    /** 最新一条算法任务；该记录从未跑过算法时为空 */
    @Schema(description = "最新一条算法任务，可为空")
    private VehicleAlgorithmTaskRespDTO latestAlgorithmTask;
}
