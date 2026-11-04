package com.chinaunicom.smartstorage.admin.dto.task.balanceItem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

/**
 * @Description  单车结算DTO实体
 * @author zyp
 * @Date 2026-01-22
 */
@Data
public class BalanceItemUpdateReqDTO {

	@Schema(name ="id", description = "唯一标识", required = true)
	private String id;

	@Schema(name ="price", description = "标准单价", required = false)
	private BigDecimal price;

}
