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
public class TaskBalanceRespDTO {

	@Schema(name ="jobNo", description = "作业单编号", nullable = true)
	private String jobNo;

	@Schema(name ="grossWeight", description = "毛重", nullable = true)
	private BigDecimal grossWeight;

	@Schema(name ="tareWeight", description = "皮重", nullable = true)
	private BigDecimal tareWeight;

	@Schema(name ="netWeight", description = "净重", nullable = true)
	private BigDecimal netWeight;

	@Schema(name ="deductWeightTotal", description = "扣量合计", nullable = true)
	private BigDecimal deductWeightTotal;

	@Schema(name ="deductionPrice", description = "每千克扣价", nullable = true)
	private BigDecimal deductionPrice;

	@Schema(name ="deduction", description = "每千克扣重", nullable = true)
	private BigDecimal deduction;

	@Schema(name ="deductWeight", description = "现场扣重", nullable = true)
	private BigDecimal deductWeight;

	@Schema(name ="idCard", description = "账户身份证号", nullable = true)
	private String idCard;

	@Schema(name ="linkPhone", description = "账户联系方式", nullable = true)
	private String linkPhone;

	@Schema(name ="bankName", description = "开户银行", nullable = true)
	private String bankName;

	@Schema(name ="bankCode", description = "开户行号", nullable = true)
	private String bankCode;

	@Schema(name ="bankAccount", description = "开户账号", nullable = true)
	private String bankAccount;


}
