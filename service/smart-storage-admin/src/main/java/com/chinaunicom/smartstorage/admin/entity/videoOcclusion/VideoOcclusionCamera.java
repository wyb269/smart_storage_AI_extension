package com.chinaunicom.smartstorage.admin.entity.videoOcclusion;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 遮挡监测摄像头配置实体，对应表 {@code ai_occlusion_camera}。
 *
 * <p>设备连接状态与遮挡告警状态相互独立，页面展示状态由前端综合计算。</p>
 *
 * @author aiVision 功能族 · 视频遮挡检测
 */
@Data
@TableName("ai_occlusion_camera")
public class VideoOcclusionCamera implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，同时作为对外摄像头标识 */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 摄像头业务编码，如 CAM-001 */
    private String cameraCode;

    /** 安装位置名称，如「一号仓库东门」 */
    private String cameraName;

    /** 所属仓库，数据范围强制过滤维度 */
    private Long warehouseId;

    private String warehouseName;

    /** 行政区划码 */
    private String regionId;

    private Long orgId;

    /** 视频平台侧标识，非敏感 */
    private String cameraId;

    /** ONLINE / OFFLINE / UNKNOWN */
    private String deviceStatus;

    /** NORMAL / ACTIVE / RECOVERED */
    private String occlusionStatus;

    private Boolean enabled;

    private Integer sortNo;

    private LocalDateTime lastHeartbeatAt;

    private LocalDateTime latestEventAt;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    private String remark;
}
