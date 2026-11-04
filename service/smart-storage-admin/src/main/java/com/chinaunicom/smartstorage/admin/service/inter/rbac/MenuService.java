package com.chinaunicom.smartstorage.admin.service.inter.rbac;

import java.util.List;

import com.chinaunicom.smartstorage.admin.entity.rbac.menu.Menu;
import com.chinaunicom.smartstorage.common.result.ActionResult;

/**
 * 菜单信息
 *
 * @author liuchink
 * @since 2020/4/8 21:18
 */
public interface MenuService {
    ActionResult<String, String> saveMenu(Menu menu);

    ActionResult<String, String> updateMenu(Menu menu);

    ActionResult<String, String> deleteMenu(int id);

    Menu getMenuById(int id);

    /**
     * 查询所有的菜单
     *
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.rbac.Menu>
     * @author liuchink
     * @since 2017/5/2:43
     */
    List<Menu> listMenu();
}
