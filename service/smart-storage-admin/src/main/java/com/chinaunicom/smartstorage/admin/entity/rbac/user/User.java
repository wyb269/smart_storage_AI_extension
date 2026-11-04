package com.chinaunicom.smartstorage.admin.entity.rbac.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chinaunicom.smartstorage.admin.entity.rbac.Organization;
import com.chinaunicom.smartstorage.admin.entity.rbac.Region;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.RoleGroup;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@TableName("base_user_info")
@Data
public class User {

    /**
     * id
     */
    @TableId(type = IdType.INPUT)
    private String id;
    private String orgId;
    private String roleGroupId;
    private String regionId;
    /**
     * 账号
     */
    private String loginId;
    /**
     * 密码
     */
    private String loginPwd;
    /**
     * 姓名
     */
    private String name;
    /**
     * 手机号码
     */

    private String phone;
    /**
     * 启用状态1=启用2=禁用
     */

    private Integer showState;
    /**
     * 添加人
     */
    private String addUser;

    /**
     * 添加时间
     */
    private LocalDateTime addTime;

    /**
     * 1=超级管理员，2=其他
     */
    private Integer userType;
    /**
     * 机构信息
     */
    @TableField(exist = false)
    private Organization organization;
    /**
     * 角色信息
     */
    @TableField(exist = false)
    private RoleGroup roleGroup;
    /**
     * 行政区
     */
    @TableField(exist = false)
    private Region region;
    /**
     * 修改人
     */
    private String updateUser;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
    /**
     * 最后修改密码时间
     */
    private LocalDateTime lastUpdatePwd;
    private String headUrl;
    private Integer employedStatus;
    private Integer mustUpdatePwd;
}
