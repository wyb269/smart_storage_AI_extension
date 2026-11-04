package com.chinaunicom.smartstorage.admin.dto.aggregation.customer;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CustomerQueryRespDTO {
    @Schema(name ="id", description = "唯一标识", nullable = true)
    private String id;

    @Schema(name ="customerType", description = "客户类型", nullable = true)
    private Integer customerType;

    @Schema(name ="name", description = "客户名称", nullable = true)
    private String name;

    @Schema(name ="phone", description = "联系电话", nullable = true)
    private String phone;

    @Schema(name ="card", description = "身份证号", nullable = true)
    private String card;

    @Schema(name ="address", description = "联系地址", nullable = true)
    private String address;
}
