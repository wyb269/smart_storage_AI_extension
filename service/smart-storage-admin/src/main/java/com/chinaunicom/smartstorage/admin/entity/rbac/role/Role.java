package com.chinaunicom.smartstorage.admin.entity.rbac.role;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.google.common.collect.Lists;
import com.chinaunicom.smartstorage.admin.entity.rbac.Organization;
import com.chinaunicom.smartstorage.admin.entity.rbac.Region;
import com.chinaunicom.smartstorage.admin.entity.rbac.menu.Menu;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色表
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
@TableName("base_role_info")
public class Role {
    public Role() {
        this.listMenu = Lists.newArrayList();
    }

    /**
     * id
     */
    @TableId(type = IdType.INPUT)
    private String id;
    /**
     * 机构id
     */

    private String orgId;
    /**
     * 行政区ID
     */

    private String regionId;
    /**
     * 名称
     */
    private String name;

    /**
     * 备注
     */
    private String remark;

    /**
     * 添加人
     */

    private String addUser;

    /**
     * 添加时间
     */

    private LocalDateTime addTime;

    /**
     * 菜单
     */
    @TableField(exist = false)
    private List<Menu> listMenu;
    /**
     * 权限
     */
    // @TableField(exist = false)
    // private List<Rights> listRights;
    /**
     * 菜单ID列表
     */
    @TableField(exist = false)
    private String menuId;
    /**
     * 权限ID列表
     */
    @TableField(exist = false)
    private String rightsId;
    /**
     * 修改人
     */

    private String updateUser;

    /**
     * 修改时间
     */

    private LocalDateTime updateTime;
    @TableField(exist = false)
    private Organization organization;
    @TableField(exist = false)
    private Region region;
    @TableField(exist = false)
    private List<RoleGroup> listRoleGroup;
    // @TableField(exist = false)
    // private List<Integer> listMenuIds;
    // @TableField(exist = false)
    // private List<Integer> listRightsIds;
}
