package com.chinaunicom.smartstorage.admin.service.impl.rbac;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.chinaunicom.smartstorage.admin.dto.rbac.role.RoleGroupQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.RoleGroup;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.RoleGroupLinked;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.admin.mapper.rbac.RoleGroupMapper;
import com.chinaunicom.smartstorage.admin.mapper.rbac.UserMapper;
import com.chinaunicom.smartstorage.admin.service.inter.rbac.RoleGroupService;
import com.chinaunicom.smartstorage.common.enumbean.OptEnum;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.chinaunicom.smartstorage.common.result.QueryPageResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色
 *
 * @author liuchink
 * @since 2020/4/19 21:28
 */
@Slf4j
@Service

public class RoleGroupServiceImpl implements RoleGroupService {
    @Resource
    private RoleGroupMapper roleGroupMapper;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveRoleGroup(RoleGroup roleGroup) {
        ActionResult<String, String> actionResult = check(roleGroup, OptEnum.ADD);
        if (actionResult.getStatus().isSuccess()) {
            List<RoleGroupLinked> listRoleGroupLinked = Lists.newLinkedList();
            roleGroupMapper.insert(roleGroup);
            if (roleGroup.getListRoleIds() != null && !roleGroup.getListRoleIds().isEmpty()) {
                for (String roleId : roleGroup.getListRoleIds()) {
                    RoleGroupLinked roleGroupLinked = new RoleGroupLinked();
                    roleGroupLinked.setGroupId(roleGroup.getId());
                    roleGroupLinked.setRoleId(roleId);
                    listRoleGroupLinked.add(roleGroupLinked);
                }
                roleGroupMapper.saveRoleGroupLinked(listRoleGroupLinked);
            }
            actionResult = actionResultService.callBackResult("添加成功");
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateRoleGroup(RoleGroup roleGroup) {
        ActionResult<String, String> actionResult = check(roleGroup, OptEnum.UPDATE);
        if (actionResult.getStatus().isSuccess()) {
            roleGroupMapper.updateById(roleGroup);
            roleGroupMapper.deleteRoleGroupLinked(roleGroup.getId());
            List<RoleGroupLinked> listRoleGroupLinked = Lists.newLinkedList();
            if (roleGroup.getListRoleIds() != null && !roleGroup.getListRoleIds().isEmpty()) {
                for (String roleId : roleGroup.getListRoleIds()) {
                    RoleGroupLinked roleGroupLinked = new RoleGroupLinked();
                    roleGroupLinked.setGroupId(roleGroup.getId());
                    roleGroupLinked.setRoleId(roleId);
                    listRoleGroupLinked.add(roleGroupLinked);
                }
                roleGroupMapper.saveRoleGroupLinked(listRoleGroupLinked);
            }
            actionResult = actionResultService.callBackResult("修改成功");
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteRoleGroup(RoleGroup roleGroup) {
        final Long count =
            userMapper.selectCount(Wrappers.lambdaQuery(User.class).eq(User::getRoleGroupId, roleGroup.getId()));
        if (count > 0) {
            return actionResultService.callBackResultFail("角色组被用户绑定不能删除");
        }
        roleGroupMapper.deleteById(roleGroup.getId());
        roleGroupMapper.deleteRoleGroupLinked(roleGroup.getId());
        return actionResultService.callBackResult("删除成功");
    }

    @Override
    public QueryPageResult<RoleGroup> listRoleGroupByConditionOnPage(RoleGroupQueryReqDTO roleGroupQueryReqDTO) {
        QueryPageResult<RoleGroup> queryPageResult = new QueryPageResult<>();
        final Long count = roleGroupMapper.listRoleGroupByConditionOnPage_COUNT(roleGroupQueryReqDTO);
        queryPageResult.setTotal(count);
        if (count > 0) {
            final List<RoleGroup> listRoleGroup = roleGroupMapper.listRoleGroupByConditionOnPage(roleGroupQueryReqDTO);
            queryPageResult.setData(listRoleGroup);
        }
        return queryPageResult;
    }

    @Override
    public RoleGroup getRoleGroupById(String id) {
        return roleGroupMapper.selectById(id);
    }

    @Override
    public RoleGroup getRoleGroupByDetail(String id) {
        return roleGroupMapper.getRoleGroupByDetail(id);
    }

    /**
     * 重复验证
     *
     * @param roleGroup Role
     * @return com.liuchink.chinkframe.admin.common.result.ActionResult
     * @author liuchink
     * @since 2018/8/12 10:57
     */
    private ActionResult<String, String> check(RoleGroup roleGroup, OptEnum optEnum) {
        RoleGroup roleGroupByName = roleGroupMapper
            .selectOne(Wrappers.lambdaQuery(RoleGroup.class).eq(RoleGroup::getName, roleGroup.getName()));
        if (roleGroupByName != null && !StrUtil.equals(roleGroupByName.getId(), roleGroup.getId())) {
            return actionResultService.callBackResultFail("角色名称重复");
        }
        return actionResultService.callBackResult();
    }
}
