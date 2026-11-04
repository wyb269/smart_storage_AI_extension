package com.chinaunicom.smartstorage.admin.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liugang
 * @since 2026/1/23 8:38
 */
@Data
public class SwipeCardRespDTO {
    @Schema(description = "智能卡ID", nullable = true, example = "12345")
    private String id;
    @Schema(description = "智能卡编号", nullable = true, example = "12345")
    private String cardNo;
    @Schema(description = "物理卡编号", nullable = true, example = "12345")
    private String physicalCardNumber;
    @Schema(description = "发卡状态1=已发卡；2=未发卡", nullable = true, example = "12345")
    private Integer state;
    @Schema(description = "作业ID", nullable = true, example = "12345")
    private String taskId;
    @Schema(description = "作业编号", nullable = true, example = "12345")
    private String jobNo;
    @Schema(
        description = "logIn=已登入；sampled=已仟样；inspected=已质检；weighed=已称毛重；warehoused=已值仓；tareWeighed=已称皮重；settled=已结算；logout=已登出",
        nullable = true, example = "12345")
    private String jobStatus;
    @Schema(description = "作业类型1=入库;2=出库", nullable = true, example = "12345")
    private Integer jobType;
}
