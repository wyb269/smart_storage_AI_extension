package com.chinaunicom.smartstorage.admin.dto.aggregation.sysconfig;

import com.chinaunicom.smartstorage.admin.dto.sso.CheckUserPasswordReqDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author liuchink
 * @since 2025/1/9 14:16
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class SystemConfigGroupAndKeyDTO extends CheckUserPasswordReqDTO {
    @Schema(description = "分组", nullable = true, example = "12345")
    private String sycGroup;
    @Schema(description = "键名", nullable = true, example = "12345")
    private String sycKey;
}
