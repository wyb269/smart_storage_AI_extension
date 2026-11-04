package com.chinaunicom.smartstorage.admin.entity.doorAnomaly;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 仓门配置实体。
 *
 * <p>对应表 {@code ai_door_config}（逻辑模型 27.1）。</p>
 *
 * <p><b>安全约束</b>：本表不存放摄像头账号、密码或凭据，
 * 凭据由视频网关侧持有（第 29.1 节）。</p>
 *
 * @author 仓门异动功能
 */
@Data
@TableName("ai_door_config")
public class AiDoorConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，雪花 ID 由应用层生成 */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 仓门业务编码 */
    private String doorCode;

    /** 仓门名称 */
    private String doorName;

    /** 仓库标识（跨系统逻辑关联，不建外键） */
    private Long warehouseId;

    /** 仓库名称（冗余，避免跨库联查） */
    private String warehouseName;

    /** 行政区划码，用于数据范围过滤 */
    private String regionId;

    /** 组织机构标识 */
    private Long orgId;

    /** 摄像头标识（非敏感） */
    private String cameraId;

    /** 摄像头展示编号 */
    private String cameraCode;

    /** 设备连接状态 ONLINE/OFFLINE/UNKNOWN */
    private String deviceStatus;

    /** 仓门物理状态 OPEN/CLOSED/UNKNOWN */
    private String doorPhysicalStatus;

    /** 告警状态 NORMAL/ACTIVE/RECOVERED */
    private String alarmStatus;

    /** 是否启用 */
    private Boolean enabled;

    /** 展示顺序 */
    private Integer sortNo;

    /** 最近心跳时间 */
    private LocalDateTime lastHeartbeatAt;

    /** 最近异动时间 */
    private LocalDateTime latestEventAt;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    private String remark;
}