package com.chinaunicom.smartstorage.admin.dto.aggregation.ditc;

import org.hibernate.validator.constraints.Range;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 字典
 *
 * @author liuchink
 * @since 2022/6/29 17:18
 */
@Data
public class DictTypeSaveReqDTO {

    /**
     * 字典名称
     */
    @NotBlank(message = "字典名称不能为空")
    @Schema(name = "name", description = "字典名称", nullable = true, example = "12345")
    private String name;

    /**
     * 字典类型
     */
    @NotBlank(message = "字典类型不能为空")
    @Schema(name = "type", description = "字典类型", nullable = true, example = "12345")
    private String type;

    /**
     * 状态1=启用、2=停用
     */
    @NotNull(message = "状态不能为空")
    @Range(min = 1, max = 2, message = "状态超出范围")
    @Schema(name = "showState", description = "状态1=启用、2=停用", nullable = true, example = "12345")
    private Integer showState;

    /**
     * 备注
     */
    @Schema(name = "remark", description = "备注", nullable = true, example = "12345")
    private String remark;
}
