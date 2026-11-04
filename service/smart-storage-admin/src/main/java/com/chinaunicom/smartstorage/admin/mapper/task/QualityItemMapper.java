package com.chinaunicom.smartstorage.admin.mapper.task;



import com.chinaunicom.smartstorage.admin.dto.task.qualityItem.QualityItemQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.qualityItem.QualityItemQueryRespDTO;
import com.chinaunicom.smartstorage.admin.entity.task.QualityItem;
import java.util.List;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 质检表(TaskFlowQualityItem)表数据库访问层
 *
 * @author makejava
 * @since 2026-01-27 11:13:50
 */
public interface QualityItemMapper extends BaseMapper<QualityItem>{

    List<QualityItem> selectByTaskId(String taskId);

    List<QualityItemQueryRespDTO> listQualityItemByCondition(QualityItemQueryReqDTO qualityItemQueryReqDTO);

    QualityItem getQualityItemByTaskId(String taskId);
}

