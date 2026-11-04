package com.chinaunicom.smartstorage.admin.mapper.rbac;

import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.admin.dto.rbac.role.RoleQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.Role;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.RoleMenuLinked;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色信息
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */

public interface RoleMapper extends MPJBaseMapper<Role> {

    /**
     * 查询
     *
     * @param roleQueryReqDTO RoleQueryReqDTO
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.rbac.Role>
     * @author liuchink
     * @since 2020/4/26 20:39
     */
    List<Role> listRoleByConditionOnPage(RoleQueryReqDTO roleQueryReqDTO);

    Long listRoleByConditionOnPage_COUNT(RoleQueryReqDTO roleQueryReqDTO);

    /**
     * 根据角色id查询菜单集合
     *
     * @param id di
     * @return com.liuchink.chinkframe.cloud.entity.Role
     * @author liuchink
     * @since 2017/5/17 22:49
     */
    Role listRoleByIdForMenu(@Param("id") String id);


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

    Role getRoleByDetail(@Param("id") String id);
}
