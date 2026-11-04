package com.chinaunicom.smartstorage.admin.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author liugang
 * @since 2026/1/23 8:38
 */
@Data
public class SwipeCardReqDTO {
    @Schema(description = "物理卡编号", nullable = true, example = "12345")
    @NotBlank(message = "物理卡编号不能为空")
    private String physicalCardNumber;
    @Schema(
        description = "刷卡类型logIn=登入；sampled=仟样；inspected=质检；weighed=称重；warehoused=值仓；settled=结算；logout=登出",
        nullable = true, example = "12345")
    @NotBlank(message = "刷卡类型不能为空")
    private String processStep;
}
