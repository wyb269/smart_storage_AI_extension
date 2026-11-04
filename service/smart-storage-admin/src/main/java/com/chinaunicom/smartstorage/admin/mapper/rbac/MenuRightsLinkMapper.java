package com.chinaunicom.smartstorage.admin.mapper.rbac;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinaunicom.smartstorage.admin.entity.rbac.menu.MenuRightsLinked;

import java.util.List;

/**
 * 菜单信息
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */

public interface MenuRightsLinkMapper extends BaseMapper<MenuRightsLinked> {
    void saveMenuRightsLinked(List<MenuRightsLinked> listMenuRightsLinked);
}
