package com.chinaunicom.smartstorage.admin.dto.rbac.login;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * 登录日志
 *
 * @author liuchink
 * @since 2022/2/23 16:05
 */
@Data
public class LoginLogRespDTO {
    /**
     * 主键
     */
    @Schema(description = "id", nullable = true, example = "123456")
    private String id;

    /**
     * 日志名称
     */
    @Schema(description = "登录类型", nullable = true, example = "123456")
    private String logName;
    /**
     * 用户ID
     */
    @Schema(description = "用户ID", nullable = true, example = "123456")
    private String userId;
    /**
     * 登录账号
     */
    @Schema(description = "登录账号", nullable = true, example = "123456")
    private String loginId;
    /**
     * 姓名
     */
    @Schema(description = "姓名", nullable = true, example = "123456")
    private String name;
    /**
     * 机构ID
     */
    @Schema(description = "机构ID", nullable = true, example = "123456")
    private String orgId;
    @Schema(description = "机构名称", nullable = true, example = "123456")
    private String orgName;
    /**
     * 行政区ID
     */
    @Schema(description = "行政区ID", nullable = true, example = "123456")
    private String regionId;
    @Schema(description = "行政区名称", nullable = true, example = "123456")
    private String regionName;
    @Schema(description = "角色组ID", nullable = true, example = "123456")
    private String roleGroupId;
    @Schema(description = "角色组名称", nullable = true, example = "123456")
    private String roleGroupName;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间", nullable = true, example = "123456")
    private LocalDateTime addTime;

    /**
     * 具体消息
     */
    // @Schema(description = "内容", nullable = true, example = "123456")
    // private String message;

    /**
     * 登录ip
     */
    @Schema(description = "ip", nullable = true, example = "123456")
    private String ip;
    @Schema(description = "登录结果", nullable = true, example = "123456")
    private String stateMessage;
    @Schema(description = "登录状态", nullable = true, example = "123456")
    private Integer loginState;
}
