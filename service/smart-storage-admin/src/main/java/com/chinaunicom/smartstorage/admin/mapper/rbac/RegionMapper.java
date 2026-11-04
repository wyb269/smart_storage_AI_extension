package com.chinaunicom.smartstorage.admin.mapper.rbac;

import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.admin.dto.rbac.region.RegionQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.Region;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 行政区
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */

public interface RegionMapper extends MPJBaseMapper<Region> {
    /**
     * 获取所有子节点信息
     *
     * @param id id
     * @param levels levels
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.rbac.OrganizationInfo>
     * @author liuchink
     * @since 2020/4/5 13:14
     */
    List<Region> listRegionChildByIdAndLevels(@Param("id") String id, @Param("levels") Integer levels);

    /**
     * 查询
     *
     * @param id id
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.rbac.OrganizationInfo>
     * @author liuchink
     * @since 2020/4/5 13:14
     */
    List<Region> listRegionChildById(@Param("id") String id);

    /**
     * 查询
     *
     * @param id id
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.rbac.OrganizationInfo>
     * @author liuchink
     * @since 2020/4/5 13:14
     */
    List<Region> listRegionParentById(@Param("id") String id);

    List<Region> listRegionByConditionOnPage(RegionQueryReqDTO regionQueryReqDTO);
}
