package com.chinaunicom.smartstorage.admin.service.inter.task;

import com.chinaunicom.smartstorage.admin.dto.task.qualityItem.QualityItemQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.qualityItem.QualityItemQueryRespDTO;
import com.chinaunicom.smartstorage.admin.entity.task.QualityItem;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;
/**
 * @Description  质量检验
 * @author zhangy
 * @Date 2026-01-29
 */
public interface QualityItemService {
    ActionResult<String, String> saveQualityItem(QualityItem qualityItem);

    ActionResult<String, String> updateQualityItem(QualityItem qualityItem);

    ActionResult<String, String> deleteQualityItem(List<QualityItem> qualityItemList);

    QualityItem getQualityItemById(String id);

    List<QualityItemQueryRespDTO> listQualityItemByConditionOnPage(QualityItemQueryReqDTO qualityItemQueryReqDTO);

    QualityItem getQualityItemByTaskId(String taskId);
}
