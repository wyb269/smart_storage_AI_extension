package com.chinaunicom.smartstorage.admin.service.impl.rbac;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.component.SystemConfigComponent;
import com.chinaunicom.smartstorage.admin.dto.rbac.user.UserQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.SystemConfig;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.admin.mapper.rbac.UserMapper;
import com.chinaunicom.smartstorage.admin.service.inter.rbac.UserService;
import com.chinaunicom.smartstorage.common.enumbean.OptEnum;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户
 *
 * @author liuchink
 * @since 2020/4/19 21:31
 */
@Slf4j
@Service

public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private SystemConfigComponent systemConfigComponent;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveUser(User user) {
        ActionResult<String, String> actionResult = check(user, OptEnum.ADD);
        if (actionResult.getStatus().isSuccess()) {
            // 保存用户表
            userMapper.insert(user);
            actionResult = actionResultService.callBackResult("添加成功");
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateUser(User user) {
        ActionResult<String, String> actionResult = check(user, OptEnum.UPDATE);
        if (actionResult.getStatus().isSuccess()) {
            userMapper.updateById(user);
            actionResult = actionResultService.callBackResult("修改成功");
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteUser(List<User> listUser) {
        userMapper.deleteByIds(listUser);
        return actionResultService.callBackResult("删除成功");
    }

    @Override
    public List<User> listUserByConditionOnPage(UserQueryReqDTO userQueryReqDTO) {
        return userMapper.listUserByConditionOnPage(userQueryReqDTO);
    }

    @Override
    public User getUserById(String id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getUserByDetail(String id) {
        return userMapper.getUserByDetail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateState(User user) {
        ActionResult<String, String> actionResult = actionResultService.callBackResult("用户状态修改成功");
        userMapper.updateById(user);
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> restUserPassword(User user) {
        user = userMapper.selectById(user.getId());
        if (user != null) {
            SystemConfig systemConfig = systemConfigComponent.getAdminResetUserPwd();
            user.setLoginPwd(SecureUtil.md5(systemConfig.getSycValue()));
            user.setLastUpdatePwd(LocalDateTime.now());
            user.setMustUpdatePwd(1);
            userMapper.updateUserLoginPassword(user);
            return actionResultService.callBackResult("密码已成功重置,请提示用户尽快修改");
        }
        return actionResultService.callBackResultFail("没有找到此用户信息");
    }

    /**
     * 重复项检查
     *
     * @param user User
     * @return com.liuchink.chinkframe.admin.common.result.ActionResult
     * @author liuchink
     * @since 2018/8/12 11:17
     */
    private ActionResult<String, String> check(User user, OptEnum optEnum) {
        User userAuthByLoginId =
            userMapper.selectOne(Wrappers.lambdaQuery(User.class).eq(User::getLoginId, user.getLoginId()));
        if (userAuthByLoginId != null && !StrUtil.equals(userAuthByLoginId.getId(), user.getId())) {
            return actionResultService.callBackResultFail("用户登录账号不能重复");
        }
        return actionResultService.callBackResult();
    }
}
