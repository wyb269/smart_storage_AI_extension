package com.chinaunicom.smartstorage.admin.dto.task.warehouseItem;

import com.chinaunicom.smartstorage.common.bean.BaseQueryDTO;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @Description  值仓DTO实体
 * @author zyp
 * @Date 2026-01-22
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class WarehouseItemQueryReqDTO extends BaseQueryDTO {

	@Hidden
	private String startTime;

	@Hidden
	private String endTime;
}
