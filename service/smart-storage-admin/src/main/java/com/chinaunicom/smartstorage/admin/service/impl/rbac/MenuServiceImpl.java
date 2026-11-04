package com.chinaunicom.smartstorage.admin.service.impl.rbac;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.component.MenuTreeComponent;
import com.chinaunicom.smartstorage.admin.entity.rbac.menu.Menu;
import com.chinaunicom.smartstorage.admin.mapper.rbac.MenuMapper;
import com.chinaunicom.smartstorage.admin.mapper.rbac.MenuRightsMapper;
import com.chinaunicom.smartstorage.admin.mapper.rbac.RoleMenuLinkedMapper;
import com.chinaunicom.smartstorage.admin.service.inter.rbac.MenuService;
import com.chinaunicom.smartstorage.common.enumbean.OptEnum;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 菜单
 *
 * @author liuchink
 * @since 2020/4/8 21:19
 */
@Service
@Slf4j
public class MenuServiceImpl implements MenuService {
    @Resource
    private MenuMapper menuMapper;
    @Resource
    private RoleMenuLinkedMapper roleMenuLinkedMapper;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private MenuRightsMapper menuRightsMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveMenu(Menu menu) {
        ActionResult<String, String> actionResult = check(menu, OptEnum.ADD);
        if (actionResult.getStatus().isSuccess()) {
            menuMapper.insert(menu);
            actionResult = actionResultService.callBackResult("添加成功");
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateMenu(Menu menu) {
        menuMapper.updateById(menu);
        return actionResultService.callBackResult("修改成功");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteMenu(int id) {
        final List<Integer> listIds = menuMapper.listMenuByIdWithChild(id);
        if (CollUtil.isNotEmpty(listIds)) {
            roleMenuLinkedMapper.deleteRoleMenuLinkedByMenuId(listIds);
        }
        menuMapper.deleteByIds(listIds);
        // menuRightsMapper.delete(Wrappers.lambdaQuery(MenuRights.class).in(MenuRights::getMenuId, listIds));
        return actionResultService.callBackResult("删除成功");
    }

    @Override
    public Menu getMenuById(int id) {
        return menuMapper.selectById(id);
    }

    @Override
    public List<Menu> listMenu() {
        final List<Menu> listMenu = menuMapper.listMenu();
        MenuTreeComponent menuTreeComponent = new MenuTreeComponent(listMenu);
        return menuTreeComponent.builTree();
    }

    private ActionResult<String, String> check(Menu menu, OptEnum optEnum) {
        Menu menuId = menuMapper.selectOne(Wrappers.lambdaQuery(Menu.class).eq(Menu::getId, menu.getId()));
        if (menuId != null && menuId.getId().equals(menu.getId())) {
            return actionResultService.callBackResultFail("菜单编号不能重复");
        }
        return actionResultService.callBackResult();
    }
}
