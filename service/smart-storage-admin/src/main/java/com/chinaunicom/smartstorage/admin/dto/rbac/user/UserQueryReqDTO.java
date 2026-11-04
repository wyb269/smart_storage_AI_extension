package com.chinaunicom.smartstorage.admin.dto.rbac.user;

import com.chinaunicom.smartstorage.common.bean.BaseQueryDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 用户查询条件
 *
 * @author liuchink
 * @since 2017/12/18 11:54
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class UserQueryReqDTO extends BaseQueryDTO {

    /**
     * 名称
     */
    @Schema(description = "名称", nullable = true, example = "name")
    private String name;

}
