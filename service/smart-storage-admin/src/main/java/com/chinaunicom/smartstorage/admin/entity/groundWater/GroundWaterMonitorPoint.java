package com.chinaunicom.smartstorage.admin.entity.groundWater;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积水监测点位配置实体，对应表 {@code ai_water_monitor_point}。
 *
 * <p>对应原型图左侧「监测点位分布」网格的一张卡片。卡片标题是点位名
 * （主通廊东侧 / 主通廊西侧 …），点位再绑定一路摄像头供右侧实时监控。</p>
 *
 * <p>设备连接状态与积水告警状态相互独立，页面展示状态由前端综合计算。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
@Data
@TableName("ai_water_monitor_point")
public class GroundWaterMonitorPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，同时作为对外点位标识 */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 点位名称，如「主通廊东侧」，左侧卡片标题 */
    private String pointName;

    /** 所属仓库，数据范围强制过滤维度 */
    private Long warehouseId;

    private String warehouseName;

    /** 行政区划码 */
    private String regionId;

    private Long orgId;

    /**
     * 绑定的视频平台侧摄像头标识，非敏感。
     *
     * <p>对应列 {@code camera_id}。点位与摄像头是<b>配置绑定关系</b>：
     * 页面点的是点位卡片，右侧取流用的是这里记的摄像头。
     * 与遮挡那版 {@code ai_occlusion_camera.camera_id} 同一语义。</p>
     */
    private String cameraId;

    /** 摄像头业务编码，如 CAM-001，卡片与记录表都要展示 */
    private String cameraCode;

    /** ONLINE / OFFLINE / UNKNOWN */
    private String deviceStatus;

    /** NORMAL / ACTIVE / RECOVERED；ACTIVE 即页面「积水」 */
    private String alarmStatus;

    private Boolean enabled;

    private Integer sortNo;

    /** 最近心跳；超过 90 分钟前端判为离线 */
    private LocalDateTime lastHeartbeatAt;

    private LocalDateTime latestEventAt;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    private String remark;
}
