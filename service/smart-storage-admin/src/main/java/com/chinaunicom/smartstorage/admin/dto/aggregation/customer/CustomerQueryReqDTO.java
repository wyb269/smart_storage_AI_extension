package com.chinaunicom.smartstorage.admin.dto.aggregation.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CustomerQueryReqDTO {
    @Schema(name ="customerType", description = "客户类型", required = false)
    private Integer customerType;

    @Schema(name ="name", description = "客户名称", required = false)
    private String name;

    @Schema(name ="card", description = "身份证号", required = false)
    private String card;

    @Schema(name ="inStatus", description = "入库通知单状态", required = false)
    private Integer inStatus;
    @Schema(name ="outStatus", description = "出库通知单状态", required = false)
    private Integer outStatus;
}
