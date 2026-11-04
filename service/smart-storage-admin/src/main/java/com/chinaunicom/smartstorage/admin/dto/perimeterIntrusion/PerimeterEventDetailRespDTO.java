package com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 周界入侵事件详情。
 *
 * <p>在列表字段之上补事件描述、持续时长与证据、处理记录，
 * 用于回答「为什么判为入侵、有什么证据」。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "周界入侵事件详情")
public class PerimeterEventDetailRespDTO extends PerimeterEventRespDTO {

    @Schema(description = "事件描述")
    private String eventDesc;

    @Schema(description = "恢复时间")
    private String recoveredAt;

    @Schema(description = "入侵持续时长（秒）")
    private Long durationSeconds;

    @Schema(description = "是否模拟数据")
    private Boolean isSimulated;

    /**
     * 事件所属防区段<b>当前</b>的布防开关 ARMED/DISARMED。
     *
     * <p>详情弹窗据此禁用「重新识别」按钮并给出原因：撤防段不做算法识别，
     * 与后端 rerunAlgorithm 的 POINT_DISARMED 业务错误同一口径。
     * 注意这是段的实时状态而非事件发生时的快照——事件发生在布防期间、
     * 段后来被撤防是正常运维形态。</p>
     */
    @Schema(description = "防区段当前布防状态 ARMED/DISARMED")
    private String defenseStatus;

    @Schema(description = "证据列表，地址为短时签名")
    private List<PerimeterEvidenceRespDTO> evidences;

    @Schema(description = "处理记录，追加式全量历史")
    private List<ProcessRecordRespDTO> processRecords;

    @Schema(description = "算法任务列表，按开始时间倒序，最多 20 条（契约 §1/§3：弹窗不再单独请求）")
    private List<PerimeterAlgorithmTaskRespDTO> algorithmTasks;

    @Schema(description = "最近一条算法任务，可为空（历史上从未跑过算法的事件）")
    private PerimeterAlgorithmTaskRespDTO latestAlgorithmTask;
}
