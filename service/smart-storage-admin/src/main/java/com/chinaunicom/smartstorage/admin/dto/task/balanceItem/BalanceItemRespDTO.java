package com.chinaunicom.smartstorage.admin.dto.task.balanceItem;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * @Description  单车结算DTO实体
 * @author zyp
 * @Date 2026-01-22
 */
@Data
public class BalanceItemRespDTO {

	@Schema(name ="id", description = "唯一标识", nullable = true)
	private String id;

	@Schema(name ="balanceNo", description = "结算单编号", nullable = true)
	private String balanceNo;

	@Schema(name ="taskId", description = "作业单ID", nullable = true)
	private String taskId;

	@Schema(name ="grossWeight", description = "毛重", nullable = true)
	private BigDecimal grossWeight;

	@Schema(name ="tareWeight", description = "皮重", nullable = true)
	private BigDecimal tareWeight;

	@Schema(name ="netWeight", description = "净重", nullable = true)
	private BigDecimal netWeight;

	@Schema(name ="deductWeightTotal", description = "扣量合计", nullable = true)
	private BigDecimal deductWeightTotal;

	@Schema(name ="preDeductPrice", description = "每千克扣价", nullable = true)
	private BigDecimal preDeductPrice;

	@Schema(name ="preDeductWeight", description = "每千克扣重", nullable = true)
	private BigDecimal preDeductWeight;

	@Schema(name ="sceneDeductWeight", description = "现场扣重", nullable = true)
	private BigDecimal sceneDeductWeight;

	@Schema(name ="price", description = "标准单价", nullable = true)
	private BigDecimal price;

	@Schema(name ="balancePrice", description = "结算价格", nullable = true)
	private BigDecimal balancePrice;

	@Schema(name ="balanceWeight", description = "结算重量", nullable = true)
	private BigDecimal balanceWeight;

	@Schema(name ="balanceAmount", description = "结算金额", nullable = true)
	private BigDecimal balanceAmount;

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

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Schema(name ="addTime", description = "添加时间", nullable = true)
	private LocalDateTime addTime;

	@Schema(name ="addUser", description = "添加人", nullable = true)
	private String addUser;

}
