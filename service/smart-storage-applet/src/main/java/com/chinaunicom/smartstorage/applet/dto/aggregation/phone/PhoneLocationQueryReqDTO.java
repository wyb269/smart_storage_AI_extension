package com.chinaunicom.smartstorage.applet.dto.aggregation.phone;

import com.chinaunicom.smartstorage.common.bean.PageQueryDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
public class PhoneLocationQueryReqDTO extends PageQueryDTO {

    @Schema(description = "是否分页", nullable = true, example = "12345")
    @NotNull(message = "是否分页不能为空")
    private Boolean count;
}
