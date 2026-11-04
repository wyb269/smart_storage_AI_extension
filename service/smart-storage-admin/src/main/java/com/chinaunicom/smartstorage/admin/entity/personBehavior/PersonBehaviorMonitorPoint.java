package com.chinaunicom.smartstorage.admin.entity.personBehavior;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 人员行为监测区域实体，对应表 {@code ai_person_monitor_point}。
 *
 * <p>对应原型图左侧「人员监测区域分布」网格的一张卡片。一个区域绑定一路摄像头，
 * 因此设备连接状态、行为告警状态与心跳都记在本行上。
 * 页面展示状态由前端 {@code computeDisplayStatus} 综合三者算出，
 * 本表不存「正常 / 人员聚集 / 离线」这种合并后的模糊字符串。</p>
 *
 * <p>{@code alarmBehaviorType} 是<b>本功能特有</b>的一列：共享基座的综合展示态只有
 * {@code ABNORMAL} 一个值，而原型的异常卡片要按当前告警的行为类型分别显示
 * 「人员聚集」「未戴安全帽」，所以告警发生时要把是哪个行为记下来供前端取文案。</p>
 *
 * <p>安全约束：严禁新增摄像头账号、密码、RTSP 凭据、永久 Token 列。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
@Data
@TableName("ai_person_monitor_point")
public class PersonBehaviorMonitorPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，同时作为对外区域标识 */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 区域业务编码，如 AREA-001 */
    private String pointCode;

    /** 区域名称，如「一号仓库前作业区」，卡片标题直接展示 */
    private String pointName;

    /** 所属仓库，数据范围强制过滤维度 */
    private Long warehouseId;

    private String warehouseName;

    /** 行政区划码 */
    private String regionId;

    private Long orgId;

    /** 绑定的设备侧摄像头标识，非敏感 */
    private String cameraId;

    /** 绑定的摄像头业务编码，如 CAM-001 */
    private String cameraCode;

    /** 视频平台侧标识，取流时用；非敏感 */
    private String platformCameraId;

    /** ONLINE / OFFLINE / UNKNOWN */
    private String deviceStatus;

    /** NORMAL / ACTIVE / RECOVERED */
    private String alarmStatus;

    /** 当前告警对应的行为类型 CROWD/NO_HELMET/FALL/INTRUSION/OTHER；无告警时为空 */
    private String alarmBehaviorType;

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
