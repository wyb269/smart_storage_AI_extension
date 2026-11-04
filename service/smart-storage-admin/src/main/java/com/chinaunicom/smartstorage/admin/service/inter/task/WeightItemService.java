package com.chinaunicom.smartstorage.admin.service.inter.task;

import com.chinaunicom.smartstorage.admin.dto.task.weight.WeightItemInFormRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.weight.WeightItemOutFormRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.weight.WeightItemQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.task.WeightItem;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

/**
 * 称重信息表(WeightItem)表服务接口
 *
 * @author liuchink
 * @since 2026-01-26 10:45:29
 */
public interface WeightItemService {
    ActionResult<String, String> saveWeightItem(WeightItem weightItem);

    ActionResult<String, String> updateWeightItem(WeightItem weightItem);

    ActionResult<String, String> deleteWeightItem(List<WeightItem> listWeightItem);

    List<WeightItem> listWeightItemByCondition(WeightItemQueryReqDTO weightItemQueryReqDTO);

    WeightItem getWeightItemById(String id);

    WeightItem getWeightItemByDetail(String id);

    WeightItem getWeightItemByTaskId(String taskId, int weightType);

    WeightItemInFormRespDTO getWeightItemByDetailForInForm(String taskId);
    WeightItemOutFormRespDTO getWeightItemByDetailForOutForm(String taskId);
}
