package com.chinaunicom.smartstorage.admin.entity.rbac.role;


import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 角色权限信息
 *
 * @author liuchink
 * @since 2017/12/18 13:33
 */
@Data
@TableName("base_role_rights")
public class RoleRightsLinked  {



    private Integer rightsId;

    private String roleId;
}
