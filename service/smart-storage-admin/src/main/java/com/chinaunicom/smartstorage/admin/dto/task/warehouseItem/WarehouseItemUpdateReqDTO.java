package com.chinaunicom.smartstorage.admin.dto.task.warehouseItem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

/**
 * @Description  值仓DTO实体
 * @author zyp
 * @Date 2026-01-22
 */
@Data
public class WarehouseItemUpdateReqDTO {

	@Schema(name ="id", description = "唯一标识", required = true)
	private String id;

	@Schema(name ="deductWeight", description = "现场扣重", required = false)
	private BigDecimal deductWeight;

	@Schema(name ="remark", description = "备注", required = false)
	private String remark;

}
