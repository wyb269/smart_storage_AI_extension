package com.chinaunicom.smartstorage.admin.mapper.task;

import com.chinaunicom.smartstorage.admin.dto.task.balanceItem.BalanceItemQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.balanceItem.BalanceItemQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.balanceItem.TaskBalanceRespDTO;
import com.chinaunicom.smartstorage.admin.entity.task.BalanceItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;

/**
 * @Description  单车结算Mapper
 * @author zyp
 * @Date 2026-01-22
 */
public interface BalanceItemMapper extends BaseMapper<BalanceItem> {

	List<BalanceItemQueryRespDTO> listBalanceItemByConditionOnPage(BalanceItemQueryReqDTO balanceItemQueryReqDTO);

	TaskBalanceRespDTO getTaskBalance(String taskId);

}
