package com.chinaunicom.smartstorage.admin.service.impl.rbac;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.chinaunicom.smartstorage.admin.component.MenuTreeComponent;
import com.chinaunicom.smartstorage.admin.dto.rbac.role.RoleQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.Rights;
import com.chinaunicom.smartstorage.admin.entity.rbac.menu.Menu;
import com.chinaunicom.smartstorage.admin.entity.rbac.menu.MenuRightsLinked;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.Role;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.RoleGroupLinked;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.RoleMenuLinked;
import com.chinaunicom.smartstorage.admin.mapper.rbac.MenuMapper;
import com.chinaunicom.smartstorage.admin.mapper.rbac.MenuRightsLinkMapper;
import com.chinaunicom.smartstorage.admin.mapper.rbac.RoleGroupLinkedMapper;
import com.chinaunicom.smartstorage.admin.mapper.rbac.RoleMapper;
import com.chinaunicom.smartstorage.admin.service.inter.rbac.RoleService;
import com.chinaunicom.smartstorage.common.enumbean.OptEnum;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.chinaunicom.smartstorage.common.result.QueryPageResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色
 *
 * @author liuchink
 * @since 2020/4/19 21:28
 */
@Slf4j
@Service

public class RoleServiceImpl implements RoleService {
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RoleGroupLinkedMapper roleGroupLinkedMapper;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private MenuMapper menuMapper;
    @Resource
    private MenuRightsLinkMapper menuRightsLinkMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveRole(Role role) {
        ActionResult<String, String> actionResult = check(role, OptEnum.ADD);
        if (actionResult.getStatus().isSuccess()) {
            roleMapper.insert(role);
            actionResult = actionResultService.callBackResult("添加成功");
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateRole(Role role) {
        ActionResult<String, String> actionResult = check(role, OptEnum.UPDATE);
        if (actionResult.getStatus().isSuccess()) {
            roleMapper.updateById(role);
            actionResult = actionResultService.callBackResult("修改成功");
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteRole(Role role) {
        final Long count = roleGroupLinkedMapper
            .selectCount(Wrappers.lambdaQuery(RoleGroupLinked.class).eq(RoleGroupLinked::getRoleId, role.getId()));
        if (count > 0) {
            return actionResultService.callBackResultFail("角色被角色组绑定不能删除");
        }
        roleMapper.deleteById(role.getId());
        roleMapper.deleteRoleMenuLinked(role.getId());
        roleGroupLinkedMapper
            .delete(Wrappers.lambdaQuery(RoleGroupLinked.class).eq(RoleGroupLinked::getRoleId, role.getId()));
        return actionResultService.callBackResult("删除成功");
    }

    @Override
    public QueryPageResult<Role> listRoleByConditionOnPage(RoleQueryReqDTO roleQueryReqDTO) {
        QueryPageResult<Role> queryPageResult = new QueryPageResult<>();
        final Long count = roleMapper.listRoleByConditionOnPage_COUNT(roleQueryReqDTO);
        queryPageResult.setTotal(count);
        if (count > 0) {
            final List<Role> listRole = roleMapper.listRoleByConditionOnPage(roleQueryReqDTO);
            queryPageResult.setData(listRole);
        }
        return queryPageResult;
    }

    @Override
    public Role getRoleById(String id) {
        return roleMapper.selectById(id);
    }

    @Override
    public Role getRoleByDetail(String id) {
        final Role roleByDetail = roleMapper.getRoleByDetail(id);
        MenuTreeComponent menuTreeComponent = new MenuTreeComponent(roleByDetail.getListMenu());
        menuTreeComponent.builTree();
        return roleByDetail;
    }

    @Override
    public Role listRoleByIdForMenu(String id) {
        return roleMapper.listRoleByIdForMenu(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateRoleByIdForMenu(Role role) {
        ActionResult<String, String> actionResult;
        // 删除角色菜单对应信息
        roleMapper.deleteRoleMenuLinked(role.getId());
        List<RoleMenuLinked> listRoleMenuLinked = Lists.newArrayList();
        List<MenuRightsLinked> listMenuRightsLinked = Lists.newArrayList();
        // 保存角色菜单对应关系表
        for (Menu menu : role.getListMenu()) {
            RoleMenuLinked roleMenuLinkedTemp = new RoleMenuLinked();
            roleMenuLinkedTemp.setMenuId(menu.getId());
            roleMenuLinkedTemp.setRoleId(role.getId());
            listRoleMenuLinked.add(roleMenuLinkedTemp);
            for (Rights rights : menu.getListRights()) {
                MenuRightsLinked menuRightsLinked = new MenuRightsLinked();
                menuRightsLinked.setMenuId(menu.getId());
                menuRightsLinked.setRightsId(rights.getId());
                menuRightsLinked.setRoleId(role.getId());
                listMenuRightsLinked.add(menuRightsLinked);
            }
        }
        if (CollUtil.isNotEmpty(listRoleMenuLinked)) {
            roleMapper.saveRoleMenuLinked(listRoleMenuLinked);
        }
        // 删除菜单权限对应信息
        menuRightsLinkMapper
            .delete(Wrappers.lambdaQuery(MenuRightsLinked.class).in(MenuRightsLinked::getRoleId, role.getId()));
        if (CollUtil.isNotEmpty(listMenuRightsLinked)) {
            menuRightsLinkMapper.saveMenuRightsLinked(listMenuRightsLinked);
        }
        actionResult = actionResultService.callBackResult("角色菜单修改成功");
        return actionResult;
    }

    /**
     * 重复验证
     *
     * @param role Role
     * @return com.liuchink.chinkframe.admin.common.result.ActionResult
     * @author liuchink
     * @since 2018/8/12 10:57
     */
    private ActionResult<String, String> check(Role role, OptEnum optEnum) {
        Role roleByName = roleMapper.selectOne(Wrappers.lambdaQuery(Role.class).eq(Role::getName, role.getName()));
        if (roleByName != null && !StrUtil.equals(roleByName.getId(), role.getId())) {
            return actionResultService.callBackResultFail("角色名称重复");
        }
        return actionResultService.callBackResult();
    }
}
