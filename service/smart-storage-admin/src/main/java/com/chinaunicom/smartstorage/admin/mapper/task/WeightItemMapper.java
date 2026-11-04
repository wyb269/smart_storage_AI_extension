package com.chinaunicom.smartstorage.admin.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinaunicom.smartstorage.admin.dto.task.weight.WeightItemInFormRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.weight.WeightItemOutFormRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.weight.WeightItemQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.task.WeightItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 称重信息表(WeightItem)表数据库访问层
 *
 * @author liuchink
 * @since 2026-01-26 10:45:29
 */
public interface WeightItemMapper extends BaseMapper<WeightItem> {
    List<WeightItem> listWeightItemByCondition(WeightItemQueryReqDTO weightItemQueryReqDTO);

    WeightItemInFormRespDTO getWeightItemByDetailForInForm(@Param("taskId") String taskId);

    WeightItemOutFormRespDTO getWeightItemByDetailForOutForm(@Param("taskId") String taskId);

    WeightItem getWeightItemByTaskId(@Param("taskId") String taskId, @Param("weightType") int weightType);

    WeightItem getWeightItemByDetail(@Param("id") String id);
}
