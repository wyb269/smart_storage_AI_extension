package com.chinaunicom.smartstorage.admin.dto.aggregation.sysconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author liuchink
 * @since 2025/1/9 14:16
 */
@Data
public class SystemConfigDTO {
    @Schema(description = "分组", nullable = true, example = "12345")
    @NotBlank(message = "分组不能为空")
    private String sycGroup;
    @Schema(description = "键名", nullable = true, example = "12345")
    @NotBlank(message = "键名不能为空")
    private String sycKey;
    @Schema(description = "键值", nullable = true, example = "12345")
    @NotBlank(message = "键值不能为空")
    private String sycValue;
    @Schema(description = "是否缓存1=是；2=否", nullable = true, example = "12345")
    private Integer needCache;
    @Schema(description = "是否需要密码1=是；2=否", nullable = true, example = "12345")
    private Integer needPwd;
    @Schema(description = "备注", nullable = true, example = "12345")
    private String remark;
}
