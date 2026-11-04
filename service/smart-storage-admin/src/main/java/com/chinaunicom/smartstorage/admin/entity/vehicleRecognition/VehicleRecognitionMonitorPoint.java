package com.chinaunicom.smartstorage.admin.entity.vehicleRecognition;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 车辆识别点位配置实体，对应表 {@code ai_vehicle_monitor_point}。
 *
 * <p>车辆功能没有「设备状态网格」语义（左侧是今日识别车辆），但仍建点位表存
 * 识别点位（东大门入口 / 西大门出口 …），每个点位绑定一路摄像头。选中一条识别记录后，
 * 右侧播放的就是该记录所属点位的摄像头，因此点位表承担「记录 → 摄像头」的桥梁。</p>
 *
 * <p>设备连接状态与告警状态相互独立，页面展示状态由前端综合计算。</p>
 *
 * <p>安全约束：严禁新增摄像头账号、密码、RTSP 凭据、永久 Token 列。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Data
@TableName("ai_vehicle_monitor_point")
public class VehicleRecognitionMonitorPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，同时作为对外点位标识 */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 点位业务编码，如 GATE-E-IN */
    private String pointCode;

    /** 点位名称，如「东大门入口」 */
    private String pointName;

    /** 所属仓库，数据范围强制过滤维度 */
    private Long warehouseId;

    private String warehouseName;

    /** 行政区划码 */
    private String regionId;

    private Long orgId;

    /** 绑定的摄像头业务编码，如 CAM-003 */
    private String cameraCode;

    /** 摄像头安装位置名称，如「东大门入口立柱」 */
    private String cameraName;

    /** 视频平台侧标识，非敏感 */
    private String cameraId;

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
