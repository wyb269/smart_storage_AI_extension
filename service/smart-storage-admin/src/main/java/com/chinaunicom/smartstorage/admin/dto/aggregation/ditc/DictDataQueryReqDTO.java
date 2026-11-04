package com.chinaunicom.smartstorage.admin.dto.aggregation.ditc;


import com.chinaunicom.smartstorage.common.bean.BaseQueryDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 字典
 *
 * @author liuchink
 * @since 2017/12/18 11:54
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class DictDataQueryReqDTO extends BaseQueryDTO {

    @Schema(name = "label", description = "字典标签", nullable = true, example = "12345")
    private String label;
    /**
     * type
     */
    @Schema(name = "type", description = "类型", nullable = true, example = "12345")
    private String type;
    /**
     * type
     */
    @Schema(name = "showState", description = "状态", nullable = true, example = "12345")
    private Integer showState;
}
