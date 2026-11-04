package com.chinaunicom.smartstorage.admin.mapper.rbac;


import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.RoleMenuLinked;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色信息
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */

public interface RoleMenuLinkedMapper extends MPJBaseMapper<RoleMenuLinked> {
    /**
     * 保存角色和菜单之间关系
     *
     * @param listRoleMenuLinked listRoleMenuLinked
     * @author liuchink
     * @since 2017/5/17 22:49
     */
    void saveRoleMenuLinked(List<RoleMenuLinked> listRoleMenuLinked);

    /**
     * 删除角色和菜单之间关系
     *
     * @param roleId roleId
     * @author liuchink
     * @since 2017/5/17 22:50
     */
    void deleteRoleMenuLinked(@Param("roleId") String roleId);

    void deleteRoleMenuLinkedByMenuId(List<Integer> listMenuIds);
}
