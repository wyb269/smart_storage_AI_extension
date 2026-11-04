package com.chinaunicom.smartstorage.admin.entity.beltMachine;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 皮带机监测点位实体，对应表 {@code ai_belt_monitor_point}。
 *
 * <p>对应原型图左侧「皮带机情况概况」网格的一张卡片。<b>监测对象是皮带机本身</b>，
 * 摄像头只是它绑定的取流手段，因此卡片标题取 {@code deviceName}。</p>
 *
 * <p>设备连接状态与行为告警状态相互独立，页面展示状态由前端综合计算。</p>
 *
 * <p><b>安全约束</b>：严禁新增摄像头账号、密码、RTSP 凭据、永久 Token 列。</p>
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
@Data
@TableName("ai_belt_monitor_point")
public class BeltMachineMonitorPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，同时作为对外点位标识 */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 皮带机业务编码，如 BELT-001 */
    private String deviceCode;

    /** 皮带机名称，如「一号皮带机」，即卡片标题 */
    private String deviceName;

    /** 安装作业区名称，如「一号仓库作业区」 */
    private String areaName;

    /** 所属仓库，数据范围强制过滤维度 */
    private Long warehouseId;

    private String warehouseName;

    /** 行政区划码 */
    private String regionId;

    private Long orgId;

    /**
     * 绑定的摄像头在视频平台侧的标识。
     *
     * <p><b>为 NULL 表示该皮带机没有绑定摄像头</b>，与
     * {@code device_status='UNKNOWN'} 一起构成页面的「未部署」态。
     * 非敏感标识，可直接下发前端。</p>
     */
    private String cameraId;

    /** 绑定摄像头业务编码，如 CAM-003，右侧实时监控标题使用 */
    private String cameraCode;

    /** 绑定摄像头位置名称，右侧实时监控标题使用 */
    private String cameraName;

    /** ONLINE / OFFLINE / UNKNOWN */
    private String deviceStatus;

    /** NORMAL / ACTIVE / RECOVERED */
    private String alarmStatus;

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
