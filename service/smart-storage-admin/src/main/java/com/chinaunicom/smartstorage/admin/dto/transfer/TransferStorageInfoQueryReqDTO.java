package com.chinaunicom.smartstorage.admin.dto.transfer;

import com.chinaunicom.smartstorage.common.bean.BaseQueryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 倒仓信息(TransferStorageInfo)表实体类
 *
 * @author liuchink
 * @since 2026-03-10 16:43:56
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class TransferStorageInfoQueryReqDTO extends BaseQueryDTO {
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
    /**
     * 移入仓库
     */
    @Schema(description = "移入仓库ID", nullable = true, example = "12345")
    private String targetWarehouseId;
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
     * 审批状态1=待提交；2=审批中；3=已驳回；4=已作废；5=审批完成；6=已撤回
     */
    @Schema(description = "审批状态1=待提交；2=审批中；3=已驳回；4=已作废；5=审批完成；6=已撤回", nullable = true, example = "12345")
    private Integer status;
    @Schema(description = "申请人", nullable = true, example = "12345")
    private String applicant;
}
