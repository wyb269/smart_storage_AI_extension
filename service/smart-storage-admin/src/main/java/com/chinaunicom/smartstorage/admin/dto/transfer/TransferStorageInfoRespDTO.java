package com.chinaunicom.smartstorage.admin.dto.transfer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 倒仓信息(TransferStorageInfo)表实体类
 *
 * @author liuchink
 * @since 2026-03-10 16:43:56
 */
@Data
public class TransferStorageInfoRespDTO {
    /**
     * id
     */
    @Schema(description = "id", nullable = true, example = "12345")
    @NotBlank(message = "物理卡编号不能为空")
    private String id;
    /**
     * 类型
     */
    @Schema(
        description = "倒仓类型1 - 粮情异常倒仓；2 - 质量分储倒仓；3 - 仓房维修倒仓；4 - 设备升级倒仓；5 - 仓房熏蒸倒仓；6 - 储备粮轮换倒仓；7 - 专项储备倒仓；8 - 应急避险倒仓；9 - 应急供应倒仓；10 - 仓容平衡倒仓；11 - 品种集中倒仓；12 - 实验性倒仓；13 - 跨库调拨倒仓",
        nullable = true, example = "12345")
    private Integer transferType;
    /**
     * 移出仓库
     */
    @Schema(description = "移出仓库ID", nullable = true, example = "12345")
    private String sourceWarehouseId;
    @Schema(description = "移出仓库名称", nullable = true, example = "12345")
    private String sourceWarehouseName;
    /**
     * 移入仓库
     */
    @Schema(description = "移入仓库ID", nullable = true, example = "12345")
    private String targetWarehouseId;
    @Schema(description = "移入仓库名称", nullable = true, example = "12345")
    private String targetWarehouseName;
    /**
     * 粮食品种
     */
    @Schema(description = "粮食品种", nullable = true, example = "12345")
    private String grainVariety;
    /**
     * 预计倒仓量(KG)
     */
    @Schema(description = "预计倒仓量(KG)", nullable = true, example = "12345")
    private BigDecimal estimatedQuantity;
    /**
     * 开始时间
     */
    @Schema(description = "开始时间", nullable = true, example = "12345")
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    @Schema(description = "结束时间", nullable = true, example = "12345")
    private LocalDateTime endTime;
    /**
     * 倒仓原因
     */
    @Schema(description = "倒仓原因", nullable = true, example = "12345")
    private String transferReason;
    /**
     * 审批状态1=待提交；2=审批中；3=已驳回；4=已作废；5=审批完成
     */
    @Schema(description = "审批状态1=待提交；2=审批中；3=已驳回；4=已作废；5=审批完成", nullable = true, example = "12345")
    private Integer status;
    /**
     * 添加人
     */
    @Schema(description = "添加人", nullable = true, example = "12345")
    private String addUser;
    @Schema(description = "添加人", nullable = true, example = "12345")
    private String addUserName;
    /**
     * 添加时间
     */
    @Schema(description = "添加时间", nullable = true, example = "12345")
    private LocalDateTime addTime;
    /**
     * 修改人
     */
    @Schema(description = "修改人", nullable = true, example = "12345")
    private String updateUser;
    /**
     * 修改时间
     */
    @Schema(description = "修改时间", nullable = true, example = "12345")
    private LocalDateTime updateTime;

}
