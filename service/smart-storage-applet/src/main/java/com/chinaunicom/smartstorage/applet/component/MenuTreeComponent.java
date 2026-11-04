package com.chinaunicom.smartstorage.applet.component;

import com.google.common.collect.Lists;
import com.chinaunicom.smartstorage.applet.entity.member.Menu;

import java.util.List;

/**
 * @author liuchink
 * @since 2023/2/23 8:47
 */
public class MenuTreeComponent {
    private final List<Menu> menuList;

    public MenuTreeComponent(List<Menu> menuList) {
        this.menuList = menuList;
    }

    public List<Menu> builTree() {
        List<Menu> treeMenus = Lists.newArrayList();
        for (Menu menuNode : getRootNode()) {
            menuNode = buildChilTree(menuNode);
            treeMenus.add(menuNode);
        }
        return treeMenus;
    }

    public Menu buildChilTree(Menu pNode) {
        List<Menu> chilMenus = Lists.newArrayList();
        for (Menu menuNode : menuList) {
            if (menuNode.getParentId() != null && menuNode.getParentId().equals(pNode.getId())) {
                chilMenus.add(buildChilTree(menuNode));
            }
        }
        pNode.setChildren(chilMenus);
        return pNode;
    }

    public List<Menu> getRootNode() {
        List<Menu> rootMenuLists = Lists.newArrayList();
        for (Menu menuNode : menuList) {
            if (menuNode.getParentId() != null && menuNode.getParentId() == 0) {
                rootMenuLists.add(menuNode);
            }
        }
        return rootMenuLists;
    }

}
