package com.chinaunicom.smartstorage.admin.mapper.rbac;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.admin.dto.rbac.role.RoleGroupQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.RoleGroup;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.RoleGroupLinked;


/**
 * 角色信息
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */

public interface RoleGroupMapper extends MPJBaseMapper<RoleGroup> {

    /**
     * 查询
     *
     * @param roleGroupQueryReqDTO roleGroupQueryReqDTO
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.rbac.RoleGroup>
     * @author liuchink
     * @since 2020/4/26 20:39
     */
    List<RoleGroup> listRoleGroupByConditionOnPage(RoleGroupQueryReqDTO roleGroupQueryReqDTO);
    Long listRoleGroupByConditionOnPage_COUNT(RoleGroupQueryReqDTO roleGroupQueryReqDTO);

    /**
     * 保存角色和菜单之间关系
     *
     * @param listRoleGroupLinked listRoleGroupLinked
     * @author liuchink
     * @since 2017/5/17 22:49
     */
    void saveRoleGroupLinked(List<RoleGroupLinked> listRoleGroupLinked);

    /**
     * 删除角色和菜单之间关系
     *
     * @param groupId groupId
     * @author liuchink
     * @since 2017/5/17 22:50
     */
    void deleteRoleGroupLinked(@Param("groupId") String groupId);

    RoleGroup getRoleGroupByDetail(@Param("id") String id);


}
