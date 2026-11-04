package com.chinaunicom.smartstorage.admin.entity.task;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * @Description  值仓实体
 * @author zyp
 * @Date 2026-01-22
 */
@Data
@TableName(value ="task_flow_warehouse_item")
public class WarehouseItem {

	/**
	 * 唯一标识.
	 */
 	@TableField(value = "id")
	private String id;

	/**
	 * 值仓单编号.
	 */
 	@TableField(value = "warehouse_no")
	private String warehouseNo;

	/**
	 * 作业单ID.
	 */
 	@TableField(value = "task_id")
	private String taskId;

	/**
	 * 扣重.
	 */
 	@TableField(value = "deduct_weight")
	private BigDecimal deductWeight;

	/**
	 * 备注.
	 */
 	@TableField(value = "remark")
	private String remark;

	/**
	 * 值仓状态（0=作废；1=正常）.
	 */
 	@TableField(value = "state")
	private Integer state;

	/**
	 * 创建人.
	 */
 	@TableField(value = "add_time")
	private LocalDateTime addTime;

	/**
	 * 创建时间.
	 */
 	@TableField(value = "add_user")
	private String addUser;

	/**
	 * 修改人.
	 */
 	@TableField(value = "update_time")
	private LocalDateTime updateTime;

	/**
	 * 修改时间.
	 */
 	@TableField(value = "update_user")
	private String updateUser;

}
