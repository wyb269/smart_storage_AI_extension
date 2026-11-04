package com.chinaunicom.smartstorage.admin.dto.task.registration;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskFlowRegistrationSaveReqDTO {

    @NotBlank(message = "承接人姓名不能为空")
    @Schema(name = "carrierName", description = "承接人姓名", example = "张三")
    private String carrierName;

    @NotBlank(message = "承接人身份证不能为空")
    @Size(max = 18, message = "身份证号最大长度不能超过18位")
    @Schema(name = "idCard", description = "承接人身份证", example = "360731196303110883")
    private String idCard;

    @NotBlank(message = "车牌类型不能为空")
    @Schema(name = "ticketType", description = "车牌类型", example = "1")
    private String ticketType;

    @NotBlank(message = "车牌号不能为空")
    @Schema(name = "licensePlate", description = "车牌号姓名", example = "黑A2VB51")
    private String licensePlate;

    @NotBlank(message = "承接人手机号不能为空")
    @Size(max = 11, message = "手机号最大长度不能超过11位")
    @Schema(name = "contactPhone", description = "承接人手机号", example = "18612312312")
    private String contactPhone;

    @NotBlank(message = "客户ID不能为空")
    @Schema(name = "customerId", description = "客户ID", example = "123123123")
    private String customerId;

    @NotBlank(message = "智能卡ID不能为空")
    @Schema(name = "smartCardId", description = "智能卡ID", example = "123123123")
    private String smartCardId;

    @Schema(name = "remark", description = "备注", nullable = true, example = "")
    private String remark;

    @NotNull(message = "作业类型（如：1入库、2出库）不能为空")
    @Schema(name = "jobType", description = "作业类型（如：1入库、2出库）", example = "1")
    private Integer jobType;

    @NotBlank(message = "登记图片不能为空")
    @Schema(name = "pic", description = "登记图片", example = "")
    private String pic;

    //@NotBlank(message = "智能卡编号不能为空")
    @Schema(name = "cardNo", description = "智能卡编号", example = "")
    private String cardNo;

//    @NotBlank(message = "通知单id不能为空")
//    @Schema(name = "orderId", description = "通知单id", example = "")
//    private String orderId;
}
