package com.chinaunicom.smartstorage.admin.dto.personBehavior;

import com.chinaunicom.smartstorage.admin.dto.personBehavior.algorithm.PersonAlgorithmTaskRespDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 人员行为事件详情。
 *
 * <p>在列表字段之上补事件描述、持续时长、聚集人数与证据、处理记录，
 * 用于回答「为什么判为违规、有什么证据」。</p>
 *
 * <p>算法任务区（{@code algorithmTasks} / {@code latestAlgorithmTask}）随详情一次带回，
 * 弹窗不再单独请求任务列表；数据为空时前端显示「暂无算法任务」。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "人员行为事件详情")
public class PersonEventDetailRespDTO extends PersonEventRespDTO {

    @Schema(description = "事件描述")
    private String eventDesc;

    @Schema(description = "恢复时间")
    private String recoveredAt;

    @Schema(description = "行为持续时长（秒）")
    private Long durationSeconds;

    @Schema(description = "是否模拟数据")
    private Boolean isSimulated;

    @Schema(description = "证据列表，地址为短时签名")
    private List<PersonEvidenceRespDTO> evidences;

    @Schema(description = "处理记录，追加式全量历史")
    private List<ProcessRecordRespDTO> processRecords;

    @Schema(description = "算法任务列表，按开始时间倒序，最多 20 条")
    private List<PersonAlgorithmTaskRespDTO> algorithmTasks;

    @Schema(description = "最近一次算法任务；无任务时为空")
    private PersonAlgorithmTaskRespDTO latestAlgorithmTask;
}
