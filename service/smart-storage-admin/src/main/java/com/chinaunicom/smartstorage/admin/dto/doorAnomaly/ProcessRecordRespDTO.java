package com.chinaunicom.smartstorage.admin.dto.doorAnomaly;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 事件处理记录对象。
 *
 * <p>对应 AI_HANDOFF.md 第 17.6 节「处理记录」。</p>
 *
 * <p><b>关键约束</b>：处理历史采用<b>追加记录</b>，
 * 不得只覆盖最后一次处理意见。因此详情接口返回的是列表，
 * 而不是单个「处理意见」字段。</p>
 *
 * @author 仓门异动功能
 */
@Data
@Schema(description = "事件处理记录")
public class ProcessRecordRespDTO {

    @Schema(description = "处理记录标识")
    private String recordId;

    @Schema(description = "处理前状态")
    private String statusBefore;

    @Schema(description = "处理后状态")
    private String statusAfter;

    @Schema(description = "处理结果 VERIFIED_TRUE 真实异常 / VERIFIED_FALSE 误报")
    private String processResult;

    @Schema(description = "处理意见（按普通文本输出，前端不得当作 HTML 渲染）")
    private String processOpinion;

    @Schema(description = "处理人（后端按登录态记录，不信任前端传入）")
    private String processedBy;

    @Schema(description = "处理时间")
    private String processedAt;

    @Schema(description = "操作来源")
    private String source;
}
