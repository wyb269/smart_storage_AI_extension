package com.chinaunicom.smartstorage.admin.entity.task;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * @Description  单车结算实体
 * @author zyp
 * @Date 2026-01-22
 */
@Data
@TableName(value ="task_flow_balance_item")
public class BalanceItem {

	/**
	 * 唯一标识.
	 */
 	@TableField(value = "id")
	private String id;

	/**
	 * 结算单编号.
	 */
 	@TableField(value = "balance_no")
	private String balanceNo;

	/**
	 * 作业单ID.
	 */
 	@TableField(value = "task_id")
	private String taskId;

	/**
	 * 标准单价.
	 */
 	@TableField(value = "price")
	private BigDecimal price;

	/**
	 * 结算价格.
	 */
 	@TableField(value = "balance_price")
	private BigDecimal balancePrice;

	/**
	 * 结算重量.
	 */
 	@TableField(value = "balance_weight")
	private BigDecimal balanceWeight;

	/**
	 * 结算金额.
	 */
 	@TableField(value = "balance_amount")
	private BigDecimal balanceAmount;

	/**
	 * 身份证号.
	 */
 	@TableField(value = "id_card")
	private String idCard;

	/**
	 * 联系方式.
	 */
 	@TableField(value = "link_phone")
	private String linkPhone;

	/**
	 * 开户银行.
	 */
 	@TableField(value = "bank_name")
	private String bankName;

	/**
	 * 开户行号.
	 */
 	@TableField(value = "bank_code")
	private String bankCode;

	/**
	 * 银行账号.
	 */
 	@TableField(value = "bank_account")
	private String bankAccount;

	/**
	 * 添加时间.
	 */
 	@TableField(value = "add_time")
	private LocalDateTime addTime;

	/**
	 * 添加人.
	 */
 	@TableField(value = "add_user")
	private String addUser;

	/**
	 * 修改时间.
	 */
 	@TableField(value = "update_time")
	private LocalDateTime updateTime;

	/**
	 * 修改人.
	 */
 	@TableField(value = "update_user")
	private String updateUser;


	@TableField(value = "gross_weight")
	private BigDecimal grossWeight;
	@TableField(value = "tare_weight")
	private BigDecimal tareWeight;
	@TableField(value = "net_weight")
	private BigDecimal netWeight;
	@TableField(value = "pre_deduct_weight")
	private BigDecimal preDeductWeight;
	@TableField(value = "scene_deduct_weight")
	private BigDecimal sceneDeductWeight;
	@TableField(value = "deduct_weight_total")
	private BigDecimal deductWeightTotal;
	@TableField(value = "pre_deduct_price")
	private BigDecimal preDeductPrice;
}
