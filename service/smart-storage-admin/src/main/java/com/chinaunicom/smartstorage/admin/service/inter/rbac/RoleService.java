package com.chinaunicom.smartstorage.admin.service.inter.rbac;

import com.chinaunicom.smartstorage.admin.dto.rbac.role.RoleQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.Role;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.QueryPageResult;

/**
 * 角色
 *
 * @author liuchink
 * @since 2020/4/19 21:26
 */
public interface RoleService {
    /**
     * 添加
     *
     * @param role Role
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2020/4/5 14:03
     */
    ActionResult<String, String> saveRole(Role role);

    /**
     * 修改
     *
     * @param role Role
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2020/4/5 14:03
     */
    ActionResult<String, String> updateRole(Role role);

    /**
     * 删除
     *
     * @param role Role
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2020/4/5 14:03
     */
    ActionResult<String, String> deleteRole(Role role);

    /**
     * 分页查询角色信息
     *
     * @param roleQueryReqDTO RoleQueryReqDTO
     * @return java.util.List<com.liuchink.chinkframe.admin.model.entity.base.Organization>
     * @author liuchink
     * @since 2018/8/21 16:17
     */
    QueryPageResult<Role> listRoleByConditionOnPage(RoleQueryReqDTO roleQueryReqDTO);

    /**
     * 根据角色ID查询角色信息
     *
     * @param id id
     * @return com.liuchink.chinkframe.cloud.entity.Role
     * @author liuchink
     * @since 2017/5/17 22:48
     */
    Role getRoleById(String id);

    Role getRoleByDetail(String id);

    /**
     * 根据角色id查询菜单集合
     *
     * @param id di
     * @return com.liuchink.chinkframe.cloud.entity.Role
     * @author liuchink
     * @since 2017/5/17 22:49
     */
    Role listRoleByIdForMenu(String id);

    /**
     * 修改角色对应的菜单信息
     *
     * @param role Role
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2021/3/23 13:34
     */

    ActionResult<String, String> updateRoleByIdForMenu(Role role);
}
