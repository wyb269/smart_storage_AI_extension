package com.chinaunicom.smartstorage.admin.service.inter.rbac;

import java.util.List;

import com.chinaunicom.smartstorage.admin.dto.rbac.region.RegionFindByNameReqDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.region.RegionQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.Region;

/**
 * 行政区
 *
 * @author liuchink
 * @since 2020/4/5 15:08
 */
public interface RegionService {

    /**
     * 获取所有子节点信息
     *
     * @param id id
     * @param levels levels
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.rbac.OrganizationInfo>
     * @author liuchink
     * @since 2020/4/5 13:14
     */
    List<Region> listRegionChildByIdAndLevels(String id, Integer levels);

    /**
     * 查询
     *
     * @param id id
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.rbac.OrganizationInfo>
     * @author liuchink
     * @since 2020/4/5 13:14
     */
    List<Region> listRegionChildById(String id);

    Region getRegionById(String id);

    /**
     * 查询
     *
     * @param id id
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.rbac.OrganizationInfo>
     * @author liuchink
     * @since 2020/4/5 13:14
     */
    List<Region> listRegionParentById(String id);

    List<Region> listRegionByName(RegionFindByNameReqDTO regionFindByNameReqDTO);;

    List<Region> listRegionByConditionOnPage(RegionQueryReqDTO regionQueryReqDTO);
}
