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
public class DictTypeQueryReqDTO extends BaseQueryDTO {

    /**
     * name
     */
    @Schema(name = "name", description = "名称", nullable = true, example = "12345")
    private String name;
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
