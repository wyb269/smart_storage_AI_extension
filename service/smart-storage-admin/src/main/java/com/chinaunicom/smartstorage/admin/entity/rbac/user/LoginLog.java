package com.chinaunicom.smartstorage.admin.entity.rbac.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * 登录日志
 *
 * @author liuchink
 * @since 2022/2/23 16:05
 */
@TableName("base_user_login_log")
@Data
public class LoginLog {
    /**
     * 主键
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 日志名称
     */

    private String logName;
    /**
     * 用户ID
     */

    private String userId;
    /**
     * 登录账号
     */

    private String loginId;
    /**
     * 姓名
     */

    private String name;
    /**
     * 机构ID
     */
    private String orgId;
    @TableField(exist = false)
    private String orgName;
    /**
     * 行政区ID
     */
    private String regionId;
    @TableField(exist = false)
    private String regionName;
    /**
     * 角色ID
     */

    private String roleGroupId;
    @TableField(exist = false)
    private String roleGroupName;
    /**
     * 创建时间
     */

    private LocalDateTime addTime;

    /**
     * 具体消息
     */
    private String message;

    /**
     * 登录ip
     */
    private String ip;
    private String stateMessage;
    private Integer loginState;
}
