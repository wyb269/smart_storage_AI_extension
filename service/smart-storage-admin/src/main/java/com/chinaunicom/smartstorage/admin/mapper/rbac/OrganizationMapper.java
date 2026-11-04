package com.chinaunicom.smartstorage.admin.mapper.rbac;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.admin.dto.rbac.organization.OrganizationQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.Organization;

/**
 * 组织机构
 *
 * @author liuchink
 * @since 2020/4/5 12:45
 */

public interface OrganizationMapper extends MPJBaseMapper<Organization> {

    /**
     * 删除
     *
     * @param id id
     * @author liuchink
     * @since 2020/4/5 13:12
     */
    void deleteOrganization(@Param("id") String id);

    /**
     * 查询
     *
     * @param organizationQueryReqDTO OrganizationQueryReqDTO
     * @return java.util.List
     * @author liuchink
     * @since 2020/4/5 13:13
     */
    List<Organization> listOrganizationByConditionOnPage(OrganizationQueryReqDTO organizationQueryReqDTO);

    /**
     * 获取所有子节点信息
     *
     * @param id id
     * @return java.util.List
     * @author liuchink
     * @since 2020/4/5 13:14
     */
    List<Organization> listOrganizationChildById(@Param("id") String id);

    /**
     * 获取所有父节点信息
     *
     * @param id id
     * @return java.util.List
     * @author liuchink
     * @since 2020/4/5 13:14
     */

    List<Organization> listOrganizationParentById(@Param("id") String id);

    /**
     * 获取所有子节点信息
     *
     * @param id id
     * @param levels levels
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.rbac.OrganizationInfo>
     * @author liuchink
     * @since 2020/4/5 13:14
     */
    List<Organization> listOrganizationChildByIdAndLevels(@Param("id") String id, @Param("levels") Integer levels);

    Organization getOrganizationByDetail(String id);
}
