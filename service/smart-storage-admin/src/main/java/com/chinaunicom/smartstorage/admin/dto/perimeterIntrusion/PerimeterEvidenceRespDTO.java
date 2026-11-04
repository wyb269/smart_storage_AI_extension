package com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 周界入侵证据响应。
 *
 * <p>{@code originalUrl} 等字段承载的是 MediaGateway 签发的<b>短时地址</b>，
 * 不是库里的受控引用原值，也不得包含凭据。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
@Data
@Schema(description = "周界入侵证据")
public class PerimeterEvidenceRespDTO {

    @Schema(description = "证据标识")
    private String evidenceId;

    @Schema(description = "证据类型")
    private String evidenceType;

    @Schema(description = "短时原始地址")
    private String originalUrl;

    /** 标注图与原图分离，不得互相覆盖 */
    @Schema(description = "短时算法标注地址")
    private String annotatedUrl;

    @Schema(description = "短时缩略图地址")
    private String thumbnailUrl;

    @Schema(description = "采集时间")
    private String capturedAt;

    @Schema(description = "摄像头编号")
    private String cameraCode;

    @Schema(description = "资源完整性摘要")
    private String contentHash;

    @Schema(description = "展示顺序")
    private Integer sortNo;
}
