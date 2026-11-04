package com.chinaunicom.smartstorage.admin.dto.rbac.rights;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 权限表
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
public class RightsRespDTO {

    /**
     * id
     */
    @Schema(description = "id", nullable = true, example = "12345")
    private Integer id;

    /**
     * 名称
     */
    @Schema(description = "名称", nullable = true, example = "12345")
    private String name;
}
