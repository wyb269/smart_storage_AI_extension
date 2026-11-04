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
public class BalanceItemQueryRespDTO {

	@Schema(name ="id", description = "唯一标识", nullable = true)
	private String id;

	@Schema(name ="cardNo", description = "智能卡编号", nullable = true)
	private String cardNo;

	@Schema(name ="licensePlate", description = "车牌号", nullable = true)
	private String licensePlate;

	@Schema(name ="carrierName", description = "承运人", nullable = true)
	private String carrierName;

	@Schema(name ="jobNo", description = "作业编号", nullable = true)
	private String jobNo;

	@Schema(name ="jobType", description = "作业类型（1=入库;2=出库）", nullable = true)
	private Integer jobType;

	@Schema(name ="balanceNo", description = "结算单编号", nullable = true)
	private String balanceNo;

	@Schema(name ="balancePrice", description = "结算价格", nullable = true)
	private BigDecimal balancePrice;

	@Schema(name ="balanceWeight", description = "结算重量", nullable = true)
	private BigDecimal balanceWeight;

	@Schema(name ="balanceAmount", description = "结算金额", nullable = true)
	private BigDecimal balanceAmount;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Schema(name ="addTime", description = "添加时间", nullable = true)
	private LocalDateTime addTime;

}
