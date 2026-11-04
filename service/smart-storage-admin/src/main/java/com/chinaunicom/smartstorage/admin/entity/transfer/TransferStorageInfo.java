package com.chinaunicom.smartstorage.admin.entity.transfer;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 倒仓信息(TransferStorageInfo)表实体类
 *
 * @author liuchink
 * @since 2026-03-11 08:38:52
 */
@Data
@TableName("sts_transfer_storage_info")
public class TransferStorageInfo {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    /**
     * 倒仓类型1 - 粮情异常倒仓；2 - 质量分储倒仓；3 - 仓房维修倒仓；4 - 设备升级倒仓；5 - 仓房熏蒸倒仓；6 - 储备粮轮换倒仓；7 - 专项储备倒仓；8 - 应急避险倒仓；9 - 应急供应倒仓；10 - *
     * 仓容平衡倒仓；11 - 品种集中倒仓；12 - 实验性倒仓；13 - 跨库调拨倒仓
     */
    @TableField(value = "transfer_type")
    private Integer transferType;
    /**
     * 移出仓库ID
     */
    @TableField(value = "source_warehouse_id")
    private String sourceWarehouseId;
    @TableField(exist = false)
    private String sourceWarehouseName;
    /**
     * 移入仓库ID
     */
    @TableField(value = "target_warehouse_id")
    private String targetWarehouseId;
    @TableField(exist = false)
    private String targetWarehouseName;
    /**
     * 粮食品种
     */
    @TableField(value = "grain_variety")
    private String grainVariety;
    /**
     * 预计倒仓量(KG)
     */
    @TableField(value = "estimated_quantity")
    private BigDecimal estimatedQuantity;
    /**
     * 开始时间
     */
    @TableField(value = "start_time")
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    @TableField(value = "end_time")
    private LocalDateTime endTime;
    /**
     * 倒仓原因
     */
    @TableField(value = "transfer_reason")
    private String transferReason;
    /**
     * 审批状态1=待提交；2=审批中；3=已驳回；4=已作废；5=审批完成
     */
    @TableField(value = "status")
    private Integer status;
    /**
     * 添加人
     */
    @TableField(value = "add_user")
    private String addUser;
    @TableField(exist = false)
    private String addUserName;
    /**
     * 添加时间
     */
    @TableField(value = "add_time")
    private LocalDateTime addTime;
    /**
     * 修改人
     */
    @TableField(value = "update_user")
    private String updateUser;
    /**
     * 修改时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;

}
