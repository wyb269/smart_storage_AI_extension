package com.chinaunicom.smartstorage.admin.dto.task.weight;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 称重信息表(WeightItem)表实体类
 *
 * @author liuchink
 * @since 2026-01-26 10:45:29
 */
@Data
public class WeightItemRespDTO {
    /**
     * id
     */
    @Schema(description = "id", nullable = true, example = "12345")
    private String id;
    /**
     * 称重类型1=毛重；2=皮重
     */
    @Schema(description = "称重类型1=毛重；2=皮重", nullable = true, example = "12345")
    private Integer weightType;
    /**
     * 重量
     */
    @Schema(description = "称重编号", nullable = true, example = "12345")
    private String weightNo;
    /**
     * 重量
     */
    @Schema(description = "重量", nullable = true, example = "12345")
    private BigDecimal weight;
    /**
     * 备注
     */
    @Schema(description = "备注", nullable = true, example = "12345")
    private String remark;
    /**
     * 车前照片
     */
    @Schema(description = "车前照片", nullable = true, example = "12345")
    private String vehicleFrontPic;
    /**
     * 车后照片
     */
    @Schema(description = "车后照片", nullable = true, example = "12345")
    private String vehicleBackPic;
    /**
     * 添加人
     */
    @Schema(description = "添加人", nullable = true, example = "12345")
    private String addUser;
    @Schema(description = "添加人", nullable = true, example = "12345")
    private String addUserName;;
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
    @Schema(description = "智能卡编号", nullable = true, example = "12345")
    private String cardNo;
    @Schema(description = "作业单编号", nullable = true, example = "12345")
    private String jobNo;
    @Schema(description = "作业单类型", nullable = true, example = "12345")
    private String jobType;
    @Schema(description = "作业单状态", nullable = true, example = "12345")
    private String jobStatus;
}
