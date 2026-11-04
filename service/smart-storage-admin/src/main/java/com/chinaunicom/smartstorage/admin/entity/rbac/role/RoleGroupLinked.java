package com.chinaunicom.smartstorage.admin.entity.rbac.role;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 角色组中间表
 *
 * @author liuchink
 * @since 2017/12/18 13:32
 */
@Data
@TableName("base_role_group_link")
public class RoleGroupLinked {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleId;
    private String groupId;
}
