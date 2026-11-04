package com.chinaunicom.smartstorage.admin.dto.doorAnomaly;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 事件证据对象。
 *
 * <p>字段严格对齐 AI_HANDOFF.md 第 20.5 节「事件证据对象」。</p>
 *
 * <p><b>安全约束</b>：地址字段返回的必须是后端签发的短时地址，
 * 不得返回摄像头凭据；原始资源与算法标注资源必须分列，
 * 标注图不得覆盖原图（第 17.4 节）。</p>
 *
 * @author 仓门异动功能
 */
@Data
@Schema(description = "事件证据")
public class EventEvidenceRespDTO {

    @Schema(description = "证据唯一标识")
    private String evidenceId;

    @Schema(description = "证据类型 NORMAL_REFERENCE/ABNORMAL_KEY_FRAME/BEFORE_FRAME/RECOVERY_IMAGE/EVENT_VIDEO")
    private String evidenceType;

    @Schema(description = "短时原始资源地址")
    private String originalUrl;

    @Schema(description = "可选的算法标注资源地址（与原始资源分离）")
    private String annotatedUrl;

    @Schema(description = "缩略图短时地址")
    private String thumbnailUrl;

    @Schema(description = "采集时间（每张证据必须展示）")
    private String capturedAt;

    @Schema(description = "摄像头编号")
    private String cameraCode;

    @Schema(description = "资源完整性摘要")
    private String contentHash;

    @Schema(description = "展示顺序")
    private Integer sortNo;
}
