package com.chinaunicom.smartstorage.admin.service.inter.rbac;

import org.apache.ibatis.annotations.Param;

import com.chinaunicom.smartstorage.admin.dto.rbac.role.RoleGroupQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.RoleGroup;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.QueryPageResult;

/**
 * 角色
 *
 * @author liuchink
 * @since 2020/4/19 21:26
 */
public interface RoleGroupService {
    /**
     * 添加
     *
     * @param roleGroup RoleGroup
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2020/4/5 14:03
     */
    ActionResult<String, String> saveRoleGroup(RoleGroup roleGroup);

    /**
     * 修改
     *
     * @param roleGroup RoleGroup
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2020/4/5 14:03
     */
    ActionResult<String, String> updateRoleGroup(RoleGroup roleGroup);

    /**
     * 删除
     *
     * @param roleGroup roleGroup
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2020/4/5 14:03
     */
    ActionResult<String, String> deleteRoleGroup(RoleGroup roleGroup);

    /**
     * 分页查询角色信息
     *
     * @param roleGroupQueryReqDTO roleGroupQueryReqDTO
     * @return java.util.List<com.liuchink.chinkframe.admin.model.entity.base.Organization>
     * @author liuchink
     * @since 2018/8/21 16:17
     */
    QueryPageResult<RoleGroup> listRoleGroupByConditionOnPage(RoleGroupQueryReqDTO roleGroupQueryReqDTO);

    /**
     * 根据角色ID查询角色信息
     *
     * @param id id
     * @return com.liuchink.chinkframe.cloud.entity.RoleGroup
     * @author liuchink
     * @since 2017/5/17 22:48
     */
    RoleGroup getRoleGroupById(String id);

    RoleGroup getRoleGroupByDetail(@Param("id") String id);
}
