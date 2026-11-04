package com.chinaunicom.smartstorage.admin.dto.task.sampleItem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * @Description 扦样信息
 * @author zhangy
 * @Date 2026-01-26
 */
@Data
public class SampleItemRespDTO {
    /**
     * 主键
     */
    @Schema(name ="id", description = "唯一标识", nullable = true)
    private String id;
    /**
     * 扦样单编号
     */
    @Schema(name ="sampleNo", description = "扦样单编号", nullable = true)
    private String sampleNo;
    /**
     * 作业单ID
     */
    @Schema(name ="taskId", description = "作业单ID", nullable = true)
    private String taskId;
    /**
     * 粮食品种(数据字典GRAIN_VARIETY)
     */
    @Schema(name ="grainVariety", description = "粮食品种", nullable = true)
    private String grainVariety;
    /**
     * 扦样方式(1 人工)
     */
    @Schema(name ="sampleType", description = "扦样方式(1 人工)", nullable = true)
    private Integer sampleType;
    /**
     * 备注
     */
    @Schema(name ="remarks", description = "备注", nullable = true)
    private String remarks;
    /**
     * 创建人（扦样员）
     */
    @Schema(name ="addUser", description = "创建人（扦样员）", nullable = true)
    private String addUser;
    /**
     * 创建时间（扦样时间）
     */
    @Schema(name ="addTime", description = "创建时间", nullable = true)
    private LocalDateTime addTime;
    /**
     * 修改人
     */
    @Schema(name ="updateUser", description = "修改人", nullable = true)
    private String updateUser;
    /**
     * 修改时间
     */
    @Schema(name ="updateTime", description = "修改时间", nullable = true)
    private LocalDateTime updateTime;
}
