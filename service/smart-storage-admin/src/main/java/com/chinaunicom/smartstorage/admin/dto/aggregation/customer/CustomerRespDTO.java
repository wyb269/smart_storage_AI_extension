package com.chinaunicom.smartstorage.admin.dto.aggregation.customer;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerRespDTO {
    @Schema(name ="id", description = "id")
    private String id;

    @Schema(name ="customerType", description = "客户类型")
    private Integer customerType;

    @Schema(name ="name", description = "客户名称")
    private String name;

    @Schema(name ="phone", description = "联系电话")
    private String phone;

    @Schema(name ="card", description = "身份证号")
    private String card;

    @Schema(name ="address", description = "联系地址")
    private String address;

    @Schema(name ="bankName", description = "开户银行")
    private String bankName;

    @Schema(name ="bankAccount", description = "银行账号")
    private String bankAccount;

    @Schema(name ="remarks", description = "备注")
    private String remarks;

    @Schema(name ="addTime", description = "创建时间")
    private LocalDateTime addTime;

    @Schema(name ="bankCode", description = "开户行号")
    private String bankCode;
}
