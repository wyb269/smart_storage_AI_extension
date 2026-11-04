package com.chinaunicom.smartstorage.admin.dto.task.warehouseItem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/**
 * @Description  值仓DTO实体
 * @author zyp
 * @Date 2026-01-22
 */
@Data
public class WarehouseItemSaveReqDTO {

	@NotBlank(message = "作业单ID不能为空")
	@Schema(name ="taskId", description = "作业单ID", required = true)
	private String taskId;

	@Schema(name ="deductWeight", description = "现场扣重", required = false)
	private BigDecimal deductWeight;

	@Schema(name ="remark", description = "备注", required = false)
	private String remark;

}
