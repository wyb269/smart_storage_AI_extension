package com.chinaunicom.smartstorage.admin.dto.aggregation.optlog;

import com.chinaunicom.smartstorage.common.bean.TimeBaseQueryDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 组织机构查询条件
 *
 * @author liuchink
 * @since 2017/12/18 11:54
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class OperationLogRecordQueryReqDTO extends TimeBaseQueryDTO {

    /**
     * title
     */
    @Schema(description = "模块标题", nullable = true, example = "123456")
    private String title;
    /**
     * 业务类型
     */
    @Schema(description = "业务类型", nullable = true, example = "123456")
    private String businessType;
    /**
     * 操作类别
     */
    @Schema(description = "操作类别", nullable = true, example = "123456")
    private String operType;
    /**
     * 操作人
     */
    @Schema(description = "操作人", nullable = true, example = "123456")
    private String operName;
    /**
     * 操作状态
     */
    @Schema(description = "操作状态", nullable = true, example = "123456")
    private String state;
}
