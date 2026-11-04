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
public class DictDataSaveReqDTO {

    /**
     * 字典排序
     */
    @NotNull(message = "字典排序不能为空")
    @Schema(name = "sort", description = "字典排序", nullable = true, example = "12345")
    private Integer sort;

    /**
     * 字典标签
     */
    @NotBlank(message = "字典标签不能为空")
    @Schema(name = "label", description = "字典标签", nullable = true, example = "12345")
    private String label;

    /**
     * 字典键值
     */
    @NotBlank(message = "字典键值不能为空")
    @Schema(name = "description", description = "字典键值", nullable = true, example = "12345")
    private String description;

    /**
     * 字典类型
     */
    @NotBlank(message = "字典类型不能为空")
    @Schema(name = "type", description = "字典类型", nullable = true, example = "12345")
    private String type;

    /**
     * 是否默认1=默认、2=非默认
     */
    @NotNull(message = "是否默认不能为空")
    @Range(min = 1, max = 2, message = "是否默认超出范围")
    @Schema(name = "isDefault", description = "是否默认1=默认、2=非默认", nullable = true, example = "12345")
    private Integer isDefault;

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
    @Schema(name = "cssClass", description = "cssClass", nullable = true, example = "12345")
    private String cssClass;
    @Schema(name = "listClass", description = "listClass", nullable = true, example = "12345")
    private String listClass;
}
