package com.chinaunicom.smartstorage.applet.dto.aggregation.ditc;


import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 字典
 *
 * @author liuchink
 * @since 2022/6/29 17:18
 */
@Data
public class DictTypeRespDTO  {

    /**
     * 字典主键
     */
    @Schema(description = "id", nullable = true, example = "12345")
    private Long id;

    /**
     * 字典名称
     */
    @Schema(description = "字典名称", nullable = true, example = "12345")
    private String name;

    /**
     * 字典类型
     */
    @Schema(description = "字典类型", nullable = true, example = "12345")
    private String type;

    /**
     * 状态1=启用、2=停用
     */
    @Schema(description = "状态1=启用、2=停用", nullable = true, example = "12345")
    private Integer showState;

    /**
     * 备注
     */
    @Schema(description = "备注", nullable = true, example = "12345")
    private String remark;

    /**
     * 字典内容
     */
    @Schema(description = "字典内容", nullable = true, example = "12345")
    private List<DictDataRespDTO> listDictData;
}
