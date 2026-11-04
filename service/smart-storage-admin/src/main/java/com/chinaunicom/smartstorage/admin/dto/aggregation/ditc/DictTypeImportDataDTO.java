package com.chinaunicom.smartstorage.admin.dto.aggregation.ditc;


import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * PhoneLocationExportData
 *
 * @author liuchink
 * @since 2022/2/18 10:46
 */
@Data
public class DictTypeImportDataDTO {

	/**
	 * 字典名称
	 */
	@ExcelProperty("名称")
	private String name;

	/**
	 * 字典类型
	 */
	@ExcelProperty("类型")
	private String type;

	/**
	 * 状态1=启用、2=停用
	 */
	@ExcelProperty("状态1=启用、2=停用")
	private Integer showState;

	/**
	 * 备注
	 */
	@ExcelProperty("备注")
	private String remark;

}
