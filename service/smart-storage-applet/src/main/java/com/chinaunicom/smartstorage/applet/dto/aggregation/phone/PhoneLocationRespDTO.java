package com.chinaunicom.smartstorage.applet.dto.aggregation.phone;



import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * 号码归属地
 *
 * @author liuchink
 * @since 2022/2/16 9:28
 */
@Data
public class PhoneLocationRespDTO  {

	/**
	 * 号码前缀
	 */
	@Schema(description = "号码前缀", nullable = true, example = "12345")
	private String perfix;

	/**
	 * 运营商
	 */
	@Schema(description = "运营商", nullable = true, example = "12345")
	private String isp;

	/**
	 * 省份
	 */
	@Schema(description = "省份", nullable = true, example = "12345")
	private String province;

	/**
	 * 城市
	 */
	@Schema(description = "城市", nullable = true, example = "12345")
	private String city;

	/**
	 * 区号
	 */
	@Schema(description = "区号", nullable = true, example = "12345")
	private String cityCode;

	/**
	 * 邮编
	 */
	@Schema(description = "邮编", nullable = true, example = "12345")
	private String postCode;
}