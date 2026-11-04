package com.chinaunicom.smartstorage.admin.dto.task.weight;

import com.chinaunicom.smartstorage.common.bean.TimeBaseQueryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 称重信息表(WeightItem)表实体类
 *
 * @author liuchink
 * @since 2026-01-26 10:45:29
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class WeightItemQueryReqDTO extends TimeBaseQueryDTO {
    /**
     * 作业单ID
     */
    @Schema(description = "作业单编号", nullable = true, example = "12345")
    private String jobNo;
    /**
     * 称重类型1=毛重；2=皮重
     */
    @Schema(description = "称重类型1=毛重；2=皮重", nullable = true, example = "12345")
    private Integer weightType;
    /**
     * 重量
     */
    @Schema(description = "重量", nullable = true, example = "12345")
    private BigDecimal weight;
    /**
     * 备注
     */
    @Schema(description = "备注", nullable = true, example = "12345")
    private String remark;

}
