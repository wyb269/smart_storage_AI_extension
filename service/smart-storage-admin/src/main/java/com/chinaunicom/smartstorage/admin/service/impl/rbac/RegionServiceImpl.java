package com.chinaunicom.smartstorage.admin.service.impl.rbac;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.dto.rbac.region.RegionFindByNameReqDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.region.RegionQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.Region;
import com.chinaunicom.smartstorage.admin.mapper.rbac.RegionMapper;
import com.chinaunicom.smartstorage.admin.service.inter.rbac.RegionService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 行政区
 *
 * @author liuchink
 * @since 2020/4/5 15:10
 */
@Service
@Slf4j
public class RegionServiceImpl implements RegionService {
    @Resource
    private RegionMapper regionMapper;

    @Override
    public List<Region> listRegionChildByIdAndLevels(String id, Integer levels) {
        return regionMapper.listRegionChildByIdAndLevels(id, levels);
    }

    @Override
    public List<Region> listRegionChildById(String id) {
        return regionMapper.listRegionChildById(id);
    }

    @Override
    public Region getRegionById(String id) {
        return regionMapper.selectById(id);
    }

    @Override
    public List<Region> listRegionParentById(String id) {
        return regionMapper.listRegionParentById(id);
    }

    @Override
    public List<Region> listRegionByName(RegionFindByNameReqDTO regionFindByNameReqDTO) {
        return regionMapper.selectList(Wrappers.lambdaQuery(Region.class)
            .like(Region::getName, regionFindByNameReqDTO.getName()).orderByDesc(Region::getId));
    }

    @Override
    public List<Region> listRegionByConditionOnPage(RegionQueryReqDTO regionQueryReqDTO) {
        return regionMapper.listRegionByConditionOnPage(regionQueryReqDTO);
    }
}
