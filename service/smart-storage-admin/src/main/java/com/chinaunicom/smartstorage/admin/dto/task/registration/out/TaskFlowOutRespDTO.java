package com.chinaunicom.smartstorage.admin.dto.task.registration.out;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskFlowOutRespDTO {
    @Schema(name = "id", description = "主键ID", example = "")
    private String id;

    @Schema(name = "carrierName", description = "承接人姓名", example = "张三")
    private String carrierName;

    @Schema(name = "idCard", description = "承接人身份证", example = "360731196303110883")
    private String idCard;

    @Schema(name = "ticketType", description = "车牌类型", example = "1")
    private String ticketType;

    @Schema(name = "licensePlate", description = "车牌号", example = "黑A2VB51")
    private String licensePlate;
    @Schema(name = "registrationNo", description = "登记编号", example = "DJ20260202001")
    private String registrationNo;
    @Schema(name = "contactPhone", description = "承接人手机号", example = "18612312312")
    private String contactPhone;

    @Schema(name = "customerId", description = "客户ID", example = "123123123")
    private String customerId;
    @Schema(name = "customerName", description = "客户名称", example = "张三")
    private String customerName;
    @Schema(name = "smartCardId", description = "智能卡ID", example = "123123123")
    private String smartCardId;

    @Schema(name = "jobType", description = "作业类型（如：1入库、2出库）", example = "1")
    private Integer jobType;
    @Schema(name = "jobStatus",
            description = "作业单状态（logIn已登入、sampled已仟样、inspected已质检、weighed已称重、warehoused已值仓、" +
                    "tareWeighed已称皮、settled已结算、logout已登出、pendCard待销卡）", example = "logIn")
    private String jobStatus;

    @Schema(name = "stayTime", description = "停留时间", example = "50")
    private Integer stayTime;

    @Schema(name = "pic", description = "登记图片", example = "123123123")
    private String pic;
    @Schema(name = "registrationUser", description = "登记人", example = "123123123")
    private String registrationUser;
    @Schema(name = "registrationTime", description = "登记时间", example = "")
    private LocalDateTime registrationTime;
    @Schema(name = "remark", description = "备注", example = "")
    private String remark;
}
