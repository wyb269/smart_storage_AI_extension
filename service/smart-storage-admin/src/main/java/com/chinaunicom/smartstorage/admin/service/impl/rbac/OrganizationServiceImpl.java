package com.chinaunicom.smartstorage.admin.service.impl.rbac;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.dto.rbac.organization.OrganizationQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.Organization;
import com.chinaunicom.smartstorage.admin.mapper.rbac.OrganizationMapper;
import com.chinaunicom.smartstorage.admin.service.inter.rbac.OrganizationService;
import com.chinaunicom.smartstorage.common.enumbean.OptEnum;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;

/**
 * 组织机构
 *
 * @author liuchink
 * @since 2020/4/5 15:10
 */
@Service
@Slf4j

public class OrganizationServiceImpl implements OrganizationService {
    @Resource
    private OrganizationMapper organizationMapper;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private MapperFacade mapperFacade;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveOrganization(Organization organization) {
        ActionResult<String, String> actionResult = check(organization, OptEnum.ADD);
        if (actionResult.getStatus().isSuccess()) {
            Organization organizationParent = organizationMapper.selectById(organization.getParentId());
            if (organizationParent != null) {
                organization.setLevels(organizationParent.getLevels() + 1);
            } else {
                return actionResultService.callBackResultFail("上级机构不存在，请重新选择");
            }
            organizationMapper.insert(organization);
            actionResult = actionResultService.callBackResult("添加成功");
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateOrganization(Organization organization) {
        ActionResult<String, String> actionResult = check(organization, OptEnum.UPDATE);
        if (actionResult.getStatus().isSuccess()) {
            organizationMapper.updateById(organization);
            actionResult = actionResultService.callBackResult("修改成功");
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteOrganization(String id) {
        ActionResult<String, String> actionResult = actionResultService.callBackResult("机构删除成功");
        if ("0".equals(id)) {
            return actionResultService.callBackResult("根机构无法删除");
        }
        organizationMapper.deleteOrganization(id);
        return actionResult;
    }

    @Override
    public List<Organization> listOrganizationByConditionOnPage(OrganizationQueryReqDTO organizationQueryReqDTO) {
        return organizationMapper.listOrganizationByConditionOnPage(organizationQueryReqDTO);
    }

    @Override
    public Organization getOrganizationById(String id) {
        return organizationMapper.selectById(id);
    }

    @Override
    public Organization getOrganizationByDetail(String id) {
        return organizationMapper.getOrganizationByDetail(id);
    }

    @Override
    public List<Organization> listOrganizationChildById(String id) {
        return organizationMapper.listOrganizationChildById(id);
    }

    @Override
    public List<Organization> listOrganizationParentById(String id) {
        return organizationMapper.listOrganizationParentById(id);
    }

    @Override
    public List<Organization> listOrganizationInfoChildByIdAndLevels(String id, Integer levels) {
        return organizationMapper.listOrganizationChildByIdAndLevels(id, levels);
    }

    /**
     * 验证
     *
     * @param organization Organization
     * @return com.liuchink.chinkframe.admin.common.result.ActionResult
     * @author liuchink
     * @since 2018/8/21 16:25
     */
    private ActionResult<String, String> check(Organization organization, OptEnum optEnum) {
        // Organization organizationCode = organizationMapper
        // .selectOne(Wrappers.lambdaQuery(Organization.class).eq(Organization::getCode, organization.getCode()));
        // if (organizationCode != null && !StrUtil.equals(organizationCode.getId(), organization.getId())) {
        // return actionResultService.callBackResultFail("机构编码重复");
        // }
        Organization organizationName = organizationMapper
            .selectOne(Wrappers.lambdaQuery(Organization.class).eq(Organization::getName, organization.getName()));
        if (organizationName != null && !StrUtil.equals(organizationName.getId(), organization.getId())) {
            return actionResultService.callBackResultFail("机构名称重复");
        }
        return actionResultService.callBackResult();
    }
}
