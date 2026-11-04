package com.chinaunicom.smartstorage.admin.dto.rbac.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

/**
 * 组织机构表
 *
 * @author liuchink
 * @since 2020/4/5 12:45
 */
@Data
public class OrganizationSaveReqDTO {

    /**
     * 父级行政区划ID
     */
    @Schema(description = "父ID", nullable = true, example = "123456")
    @NotBlank(message = "父ID不能为空")
    private String parentId;

    /**
     * 名称
     */
    @Schema(description = "名称", nullable = true, example = "123456")
    @NotBlank(message = "名称不能为空")
    private String name;

    /**
     * 编码
     */
    // @Schema(description = "编码", nullable = true, example = "123456")
    // @NotBlank(message = "编码不能为空")
    // private String code;
    /**
     * 类型
     */
    @Schema(description = "类型", nullable = true, example = "123456")
    @NotNull(message = "类型不能为空")
    private Integer type;
    /**
     * 统一编码
     */
    @Schema(description = "统一编码", nullable = true, example = "123456")
    private String unifyEncoding;

    /**
     * 描述
     */
    @Schema(description = "描述", nullable = true, example = "123456")
    private String description;

    /**
     * 企业性质
     */
    @Schema(description = "企业性质", nullable = true, example = "123456")
    private String companyNature;

    /**
     * 工商登记注册号
     */
    @Schema(description = "工商登记注册号", nullable = true, example = "123456")
    private String businessRegistrationNumber;

    /**
     * 法人姓名
     */
    @Schema(description = "法人姓名", nullable = true, example = "123456")
    private String legalPersonName;

    /**
     * 法人电话
     */
    @Schema(description = "法人电话", nullable = true, example = "123456")
    private String legalRepresentativePhoneNumber;

    /**
     * 省份
     */
    @Schema(description = "省份", nullable = true, example = "123456")
    private String province;

    /**
     * 省份名称
     */
    @Schema(description = "省份名称", nullable = true, example = "123456")
    private String provinceName;

    /**
     * 城市
     */
    @Schema(description = "城市", nullable = true, example = "123456")
    private String city;

    /**
     * 城市名称
     */
    @Schema(description = "城市名称", nullable = true, example = "123456")
    private String cityName;

    /**
     * 仓库性质
     */
    @Schema(description = "仓库性质", nullable = true, example = "123456")
    private String warehouseNature;

    /**
     * 库点设计储量
     */
    @Schema(description = "库点设计储量", nullable = true, example = "123456")
    private String designStorage;

    /**
     * 库点设计面积
     */
    @Schema(description = "库点设计面积", nullable = true, example = "123456")
    private String designArea;

    /**
     * 库区编码
     */
    @Schema(description = "库区编码", nullable = true, example = "123456")
    private String libraryAreaCode;

    /**
     * 联系人
     */
    @Schema(description = "联系人", nullable = true, example = "123456")
    private String contactPerson;

    /**
     * 联系电话
     */
    @Schema(description = "联系电话", nullable = true, example = "123456")
    private String contactPhone;
    /**
     * 详细地址
     */
    @Schema(description = "详细地址", nullable = true, example = "123456")
    private String detailedAddress;

}
