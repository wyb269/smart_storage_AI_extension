package com.chinaunicom.smartstorage.admin.dto.task.warehouseItem;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * @Description  值仓DTO实体
 * @author zyp
 * @Date 2026-01-22
 */
@Data
public class WarehouseItemQueryRespDTO {

	@Schema(name ="id", description = "唯一标识", nullable = true)
	private String id;

	@Schema(name ="cardNo", description = "智能卡编号", nullable = true)
	private String cardNo;

	@Schema(name ="licensePlate", description = "车牌号", nullable = true)
	private String licensePlate;

	@Schema(name ="warehouseId", description = "粮仓号", nullable = true)
	private String warehouseId;

	@Schema(name ="grossWeight", description = "毛重", nullable = true)
	private BigDecimal grossWeight;

	@Schema(name ="tareWeight", description = "皮重", nullable = true)
	private BigDecimal tareWeight;

	@Schema(name ="grainVariety", description = "粮食品种", nullable = true)
	private String grainVariety;

	@Schema(name ="grainGrade", description = "粮食等级", nullable = true)
	private Integer grainGrade;

	@Schema(name ="deductWeight", description = "现场扣重", nullable = true)
	private BigDecimal deductWeight;

	@Schema(name ="warehouseNo", description = "值仓单编号", nullable = true)
	private String warehouseNo;

	@Schema(name ="state", description = "值仓状态（1=正常；2=作废）", nullable = true)
	private Integer state;

	@Schema(name ="jobType", description = "作业类型（1=入库；2=出库））", nullable = true)
	private Integer jobType;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Schema(name ="addTime", description = "创建时间(yyyy-MM-dd HH:mm:ss)", nullable = true)
	private LocalDateTime addTime;

}
