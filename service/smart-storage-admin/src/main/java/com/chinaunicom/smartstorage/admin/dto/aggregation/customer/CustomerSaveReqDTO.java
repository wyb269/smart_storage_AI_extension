package com.chinaunicom.smartstorage.admin.dto.aggregation.customer;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;



@Data
public class CustomerSaveReqDTO {
    @NotNull(message = "客户类型不能为空")
    @Schema(name ="customerType", description = "客户类型(1 个人、2 企业)", required = true)
    private Integer customerType;

    @NotBlank(message = "客户名称不能为空")
    @Schema(name ="name", description = "客户名称", required = true)
    private String name;

    @NotBlank(message = "联系电话不能为空")
    @Schema(name ="phone", description = "联系电话", required = true)
    private String phone;

    @NotBlank(message = "身份证号不能为空")
    @Size(max = 18, message = "身份证号最大长度不能超过18位")
    @Schema(name ="card", description = "身份证号", required = true)
    private String card;

    @NotBlank(message = "联系人地址不能为空")
    @Schema(name ="address", description = "联系人地址", required = true)
    private String address;

    @NotBlank(message = "开户银行不能为空")
    @Schema(name ="bankName", description = "开户银行", required = true)
    private String bankName;

    @NotBlank(message = "银行账号不能为空")
    @Schema(name ="bankAccount", description = "银行账号", required = true)
    private String bankAccount;

    @Schema(name ="remarks", description = "备注", required = false)
    private String remarks;

    @Schema(name ="bankCode", description = "开户行号", required = false)
    private String bankCode;
}
