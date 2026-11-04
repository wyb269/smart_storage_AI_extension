package com.chinaunicom.smartstorage.admin.entity.rbac.menu;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 角色权限信息
 *
 * @author liuchink
 * @since 2017/12/18 13:33
 */
@Data
@TableName("base_menu_rights_link")
public class MenuRightsLinked {
    private Integer menuId;
    private Integer rightsId;
    private String roleId;

}
