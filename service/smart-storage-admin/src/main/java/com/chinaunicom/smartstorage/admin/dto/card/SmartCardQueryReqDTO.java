package com.chinaunicom.smartstorage.admin.dto.card;

import com.chinaunicom.smartstorage.common.bean.BaseQueryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author liugang
 * @since 2026/1/22 13:37
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class SmartCardQueryReqDTO extends BaseQueryDTO {
    @Schema(description = "卡号", nullable = true, example = "123456")
    private String cardNo;
    @Schema(description = "物理卡号", nullable = true, example = "123456")
    private String physicalCardNumber;
    @Schema(description = "状态1=已发卡；2=未发卡", nullable = true, example = "123456")
    private Integer state;
}
