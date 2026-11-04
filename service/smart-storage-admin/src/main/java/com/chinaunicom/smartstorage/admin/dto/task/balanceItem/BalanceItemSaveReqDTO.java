package com.chinaunicom.smartstorage.admin.dto.task.balanceItem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/**
 * @Description  单车结算DTO实体
 * @author zyp
 * @Date 2026-01-22
 */
@Data
public class BalanceItemSaveReqDTO {

	@NotBlank(message = "作业单ID不能为空")
	@Schema(name ="taskId", description = "作业单ID", required = true)
	private String taskId;

	@NotNull(message = "标准单价不能为空")
	@Schema(name ="price", description = "标准单价", required = true)
	private BigDecimal price;

}
