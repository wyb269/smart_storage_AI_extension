package com.chinaunicom.smartstorage.admin.dto.task.warehouseItem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Description  单车结算DTO实体
 * @author zyp
 * @Date 2026-01-22
 */
@Data
public class TaskWarehouseRespDTO {

	@Schema(name ="jobNo", description = "作业单编号", nullable = true)
	private String jobNo;

	@Schema(name ="warehouseId", description = "仓库ID", nullable = true)
	private String warehouseId;

	@Schema(name ="grossWeight", description = "毛重", nullable = true)
	private BigDecimal grossWeight;

	@Schema(name ="tareWeight", description = "皮重", nullable = true)
	private BigDecimal tareWeight;

	@Schema(name ="grainVariety", description = "粮食品种", nullable = true)
	private String grainVariety;

	@Schema(name ="grainGrade", description = "粮食等级", nullable = true)
	private Integer grainGrade;


}
