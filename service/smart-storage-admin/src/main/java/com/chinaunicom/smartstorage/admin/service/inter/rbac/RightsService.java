package com.chinaunicom.smartstorage.admin.service.inter.rbac;

import java.util.List;

import com.chinaunicom.smartstorage.admin.entity.rbac.Rights;


/**
 * 权限
 *
 * @author liuchink
 * @since 2020/4/19 21:24
 */
public interface RightsService {

    /**
     * 获取所有权限
     *
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.rbac.Rights>
     * @author liuchink
     * @since 2017/5/17 22:47
     */
    List<Rights> listRights();
}
