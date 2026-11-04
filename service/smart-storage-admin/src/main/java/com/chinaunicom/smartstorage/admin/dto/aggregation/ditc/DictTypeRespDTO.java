package com.chinaunicom.smartstorage.admin.dto.aggregation.ditc;


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
    @Schema(name = "id", description = "id", nullable = true, example = "12345")
    private Long id;

    /**
     * 字典名称
     */
    @Schema(name = "name", description = "字典名称", nullable = true, example = "12345")
    private String name;

    /**
     * 字典类型
     */
    @Schema(name = "type", description = "字典类型", nullable = true, example = "12345")
    private String type;

    /**
     * 状态1=启用、2=停用
     */
    @Schema(name = "showState", description = "状态1=启用、2=停用", nullable = true, example = "12345")
    private Integer showState;

    /**
     * 备注
     */
    @Schema(name = "remark", description = "备注", nullable = true, example = "12345")
    private String remark;

    /**
     * 字典内容
     */
    @Schema(name = "listDictData", description = "字典内容", nullable = true, example = "12345")
    private List<DictDataRespDTO> listDictData;
}
