package com.chinaunicom.smartstorage.admin.dto.task.taskflow;

import com.chinaunicom.smartstorage.common.bean.BaseQueryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskFlowQueryReqDTO extends BaseQueryDTO {

    @Schema(name = "jobNo", description = "作业单编号", example = "ZY20260127001")
    private String jobNo;

    @Schema(name = "jobStatus",
            description = "作业单状态（logIn已登入、sampled已仟样、inspected已质检、weighed已称重、warehoused已值仓、" +
                    "tareWeighed已称皮、settled已结算、logout已登出。）", example = "logIn")
    private String jobStatus;

    @Schema(name = "carrierName", description = "承接人姓名", example = "张三")
    private String carrierName;

    @Schema(name = "customerName", description = "客户名称", example = "张三")
    private String customerName;
    @Schema(name = "startTime", description = "开始时间", example = "")
    private LocalDateTime startTime;
    @Schema(name = "endTime", description = "结束时间", example = "")
    private LocalDateTime endTime;
}

