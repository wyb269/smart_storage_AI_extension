package com.chinaunicom.smartstorage.admin.service.impl.rbac;

import java.util.List;

import org.springframework.stereotype.Service;

import com.chinaunicom.smartstorage.admin.entity.rbac.Rights;
import com.chinaunicom.smartstorage.admin.mapper.rbac.RightsMapper;
import com.chinaunicom.smartstorage.admin.service.inter.rbac.RightsService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 权限
 *
 * @author liuchink
 * @since 2020/4/19 21:25
 */
@Slf4j
@Service
public class RightsServiceImpl implements RightsService {
    @Resource
    private RightsMapper rightsMapper;

    @Override
    public List<Rights> listRights() {
        return null;
    }
}
