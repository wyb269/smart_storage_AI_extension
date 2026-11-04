package com.chinaunicom.smartstorage.applet.dto.aggregation.ditc;

import com.chinaunicom.smartstorage.common.bean.PageQueryDTO;

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
public class DictTypeQueryReqDTO extends PageQueryDTO {

    /**
     * name
     */
    @Schema(description = "名称", nullable = true, example = "12345")
    private String name;
    /**
     * type
     */
    @Schema(description = "类型", nullable = true, example = "12345")
    private String type;
    /**
     * type
     */
    @Schema(description = "状态", nullable = true, example = "12345")
    private Integer showState;
}
