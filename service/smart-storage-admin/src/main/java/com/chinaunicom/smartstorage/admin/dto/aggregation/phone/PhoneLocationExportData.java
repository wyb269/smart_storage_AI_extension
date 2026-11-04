package com.chinaunicom.smartstorage.admin.dto.aggregation.phone;


import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * PhoneLocationExportData
 *
 * @author liuchink
 * @since 2022/2/18 10:46
 */
@Data
public class PhoneLocationExportData  {

	/**
	 * 号码前缀
	 */
	@ExcelProperty("号码前缀")
	private String perfix;

	/**
	 * 运营商
	 */
	@ExcelProperty("运营商")
	private String isp;

	/**
	 * 省份
	 */
	@ExcelProperty("省份")
	private String province;

	/**
	 * 城市
	 */
	@ExcelProperty("城市")
	private String city;

	/**
	 * 区号
	 */
	@ExcelProperty("区号")
	private String cityCode;

	/**
	 * 邮编
	 */
	@ExcelProperty("邮编")
	private String postCode;
}
