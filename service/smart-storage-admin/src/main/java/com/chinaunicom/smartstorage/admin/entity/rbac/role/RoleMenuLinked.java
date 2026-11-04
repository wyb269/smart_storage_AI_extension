package com.chinaunicom.smartstorage.admin.entity.rbac.role;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 角色菜单信息
 *
 * @author liuchink
 * @since 2017/12/18 13:32
 */
@Data
@TableName("base_role_menu")
public class RoleMenuLinked  {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleId;
    private Integer menuId;
}
