package com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 处理审计记录响应（追加式，按时间正序）。
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
@Data
@Schema(description = "周界入侵事件处理记录")
public class ProcessRecordRespDTO {

    @Schema(description = "记录标识")
    private String recordId;

    @Schema(description = "变更前状态")
    private String statusBefore;

    @Schema(description = "变更后状态")
    private String statusAfter;

    @Schema(description = "处理结果")
    private String processResult;

    @Schema(description = "处理意见；自动标记会明确标注")
    private String processOpinion;

    @Schema(description = "处理人")
    private String processedBy;

    @Schema(description = "处理时间")
    private String processedAt;

    @Schema(description = "操作来源 WEB/SYSTEM")
    private String source;
}
