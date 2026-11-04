package com.chinaunicom.smartstorage.admin.dto.task.taskflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SettlementReportQueryRespDTO {
        private String id;

        // 作业编号
        @Schema(name = "jobNo", description = "作业编号", example = "")
        private String jobNo;

        // 作业类型（入库作业/出库作业）
        @Schema(name = "jobType", description = "作业类型", example = "")
        private String jobType;

        // 客户名称
        @Schema(name = "customerName", description = "客户名称", example = "")
        private String customerName;

        // 毛重(KG)
        @Schema(name = "grossWeight", description = "毛重(KG)", example = "")
        private BigDecimal grossWeight;

        // 皮重(KG)
        @Schema(name = "tareWeight", description = "皮重(KG)", example = "")
        private BigDecimal tareWeight;

        // 承运人
        @Schema(name = "carrier", description = "承运人", example = "")
        private String carrier;

        // 每千克扣量(KG)
        @Schema(name = "deductionPerKg", description = "每千克扣量(KG)", example = "")
        private BigDecimal deductionPerKg;

        // 值仓现场扣重(KG)
        @Schema(name = "onSiteDeduction", description = "值仓现场扣重(KG)", example = "")
        private BigDecimal onSiteDeduction;

        // 净重(KG)
        @Schema(name = "netWeight", description = "净重(KG)", example = "")
        private BigDecimal netWeight;

        // 标准单价(元/千克)
        @Schema(name = "standardUnitPrice", description = "标准单价(元/千克)", example = "")
        private BigDecimal standardUnitPrice;

        // 每千克扣价(元)
        @Schema(name = "deductionPricePerKg", description = "每千克扣价(元)", example = "")
        private BigDecimal deductionPricePerKg;

        // 扣量合计(KG)
        @Schema(name = "totalDeductionWeight", description = "扣量合计(KG)", example = "")
        private BigDecimal totalDeductionWeight;

        // 结算重量(KG)
        @Schema(name = "settleWeight", description = "结算重量(KG)", example = "")
        private BigDecimal settleWeight;

        // 结算金额(元)
        @Schema(name = "settleAmount", description = "结算金额(元)", example = "")
        private BigDecimal settleAmount;

        // 结算单价(元/千克)
        @Schema(name = "settleUnitPrice", description = "结算单价(元/千克)", example = "")
        private BigDecimal settleUnitPrice;

        // 支付方式
        @Schema(name = "payType", description = "支付方式", example = "")
        private String payType = "银行卡";

        // 账户身份证
        @Schema(name = "accountIdCard", description = "账户身份证", example = "")
        private String accountIdCard;

        // 账户联系方式
        @Schema(name = "accountPhone", description = "账户联系方式", example = "")
        private String accountPhone;

        // 开户银行
        @Schema(name = "bankName", description = "开户银行", example = "")
        private String bankName;

        // 开户行号
        @Schema(name = "bankCode", description = "开户行号", example = "")
        private String bankCode;

        // 开户账号
        @Schema(name = "bankAccount", description = "开户账号", example = "")
        private String bankAccount;

        // 结算时间
        @Schema(name = "settleTime", description = "结算时间", example = "")
        private LocalDateTime settleTime;

        // 粮食等级
        @Schema(name = "grainLevel", description = "粮食等级", example = "")
        private String grainLevel;

        // 粮食品种
        @Schema(name = "grainVariety", description = "粮食品种", example = "")
        private String grainVariety;
}
