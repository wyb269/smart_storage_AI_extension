package com.chinaunicom.smartstorage.admin.service.inter.task;

import com.chinaunicom.smartstorage.admin.dto.task.sampleItem.SampleItemQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.sampleItem.SampleItemQueryRespDTO;
import com.chinaunicom.smartstorage.admin.entity.task.SampleItem;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;
/**
 * @Description 扦样信息
 * @author zhangy
 * @Date 2026-01-26
 */
public interface SampleItemService {
    ActionResult<String, String> saveSampleItem(SampleItem sampleItem);

    ActionResult<String, String> updateSampleItem(SampleItem sampleItem);

    ActionResult<String, String> deleteSampleItem(List<SampleItem> listSampleItem);

    SampleItem getSampleItemById(String id);

    List<SampleItemQueryRespDTO> listSampleItemByConditionOnPage(SampleItemQueryReqDTO sampleItemQueryReqDTO);

    SampleItem getSampleItemByTaskId(String taskId);

}
