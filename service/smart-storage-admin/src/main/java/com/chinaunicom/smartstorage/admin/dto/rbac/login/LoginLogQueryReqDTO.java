package com.chinaunicom.smartstorage.admin.dto.rbac.login;

import com.chinaunicom.smartstorage.common.bean.TimeBaseQueryDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author liuchink
 * @since 2024/08/14 10:54
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class LoginLogQueryReqDTO extends TimeBaseQueryDTO {
    /**
     * 登录账号
     */
    @Schema(description = "登录账号", nullable = true, example = "123456")
    private String loginId;
    /**
     * 名称
     */
    @Schema(description = "姓名", nullable = true, example = "123456")
    private String name;
}
