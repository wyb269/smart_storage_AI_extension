package com.chinaunicom.smartstorage.admin.mapper.task;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinaunicom.smartstorage.admin.dto.task.sampleItem.SampleItemQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.sampleItem.SampleItemQueryRespDTO;
import com.chinaunicom.smartstorage.admin.entity.task.SampleItem;
import java.util.List;

/**
 * 扦样表(TaskFlowSampleItem)表数据库访问层
 *
 * @author zhangy
 * @since 2026-01-26 09:48:21
 */
public interface SampleItemMapper extends BaseMapper<SampleItem> {

    List<SampleItemQueryRespDTO> listSampleItemByCondition(SampleItemQueryReqDTO sampleItemQueryReqDTO);

    List<SampleItem> selectByTaskId(String taskId);

    SampleItem getSampleItemByTaskId(String taskId);

}

