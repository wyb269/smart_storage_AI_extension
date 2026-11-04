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
public class WarehouseItemRespDTO {

	@Schema(name ="id", description = "唯一标识", nullable = true)
	private String id;

	@Schema(name ="warehouseNo", description = "值仓单编号", nullable = true)
	private String warehouseNo;

	@Schema(name ="taskId", description = "作业单ID", nullable = true)
	private String taskId;

	@Schema(name ="deductWeight", description = "现场扣重", nullable = true)
	private BigDecimal deductWeight;

	@Schema(name ="remark", description = "备注", nullable = true)
	private String remark;

	@Schema(name ="state", description = "值仓状态（1=正常；2=作废）", nullable = true)
	private Integer state;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	@Schema(name ="addTime", description = "创建时间(yyyy-MM-dd HH:mm:ss)", nullable = true)
	private LocalDateTime addTime;

	@Schema(name ="addUserName", description = "创建人", nullable = true)
	private String addUserName;

}
