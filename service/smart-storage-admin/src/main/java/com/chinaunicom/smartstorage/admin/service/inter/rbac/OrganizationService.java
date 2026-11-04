package com.chinaunicom.smartstorage.admin.service.inter.rbac;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.chinaunicom.smartstorage.admin.dto.rbac.organization.OrganizationQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.Organization;
import com.chinaunicom.smartstorage.common.result.ActionResult;

/**
 * 组织机构
 *
 * @author liuchink
 * @since 2020/4/5 15:08
 */
public interface OrganizationService {
    /**
     * 保存组织机构
     *
     * @param organization OrganizationPo
     * @return com.liuchink.chinkframe.admin.common.result.ActionResult
     * @author liuchink
     * @since 2018/8/21 16:16
     */
    ActionResult<String, String> saveOrganization(Organization organization);

    /**
     * 修改组织机构
     *
     * @param organization OrganizationPo
     * @return com.liuchink.chinkframe.admin.common.result.ActionResult
     * @author liuchink
     * @since 2018/8/21 16:16
     */
    ActionResult<String, String> updateOrganization(Organization organization);

    /**
     * 删除组织机构
     *
     * @param id id
     * @return com.liuchink.chinkframe.admin.common.result.ActionResult
     * @author liuchink
     * @since 2018/8/21 16:16
     */
    ActionResult<String, String> deleteOrganization(String id);

    /**
     * 分页查询组织机构
     *
     * @param organizationQueryReqDTO OrganizationQueryReqDTO
     * @return java.util.List<com.liuchink.chinkframe.admin.model.entity.base.Organization>
     * @author liuchink
     * @since 2018/8/21 16:17
     */
    List<Organization> listOrganizationByConditionOnPage(OrganizationQueryReqDTO organizationQueryReqDTO);

    /**
     * 根据组织机构ID查询机构信息
     *
     * @param id id
     * @return com.liuchink.chinkframe.admin.model.entity.base.Organization
     * @author liuchink
     * @since 2018/8/21 16:17
     */
    Organization getOrganizationById(String id);

    Organization getOrganizationByDetail(String id);

    /**
     * 获取所有子节点信息
     *
     * @param id id
     * @return java.util.List
     * @author liuchink
     * @since 2020/4/5 13:14
     */
    List<Organization> listOrganizationChildById(String id);

    /**
     * 获取所有父节点信息
     *
     * @param id id
     * @return java.util.List
     * @author liuchink
     * @since 2020/4/5 13:14
     */

    List<Organization> listOrganizationParentById(String id);

    /**
     * 获取所有子节点信息
     *
     * @param id id
     * @param levels levels
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.rbac.OrganizationInfo>
     * @author liuchink
     * @since 2020/4/5 13:14
     */
    List<Organization> listOrganizationInfoChildByIdAndLevels(@Param("id") String id, @Param("levels") Integer levels);

}
