package com.chinaunicom.smartstorage.admin.entity.perimeterIntrusion;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 周界防区段监测对象实体，对应表 {@code ai_perimeter_monitor_point}。
 *
 * <p>与遮挡那一版的 {@code ai_occlusion_camera} 同构，但监测主体由「摄像头」
 * 换成「防区段」：一个防区段绑定一路摄像头，卡片标题形如「东防区-东墙段01」。</p>
 *
 * <p>设备连接状态、布防开关、入侵告警状态三者相互独立，
 * 页面展示状态由前端综合计算（本功能还需在共享计算之上叠加布防判断）。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
@Data
@TableName("ai_perimeter_monitor_point")
public class PerimeterIntrusionMonitorPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，同时作为对外防区段标识 */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 防区段业务编码，如 PD-001 */
    private String pointCode;

    /** 防区段名称，如「东防区-东墙段01」 */
    private String pointName;

    /** 所属防区 EAST/WEST/SOUTH/NORTH，网格筛选下拉的过滤维度 */
    private String zone;

    /** 防区展示名，如「东区」 */
    private String zoneName;

    /** 墙段名称，如「东墙段01」 */
    private String segmentName;

    /** 所属仓库，数据范围强制过滤维度 */
    private Long warehouseId;

    private String warehouseName;

    /** 行政区划码 */
    private String regionId;

    private Long orgId;

    /** 绑定的摄像头配置标识 */
    private Long cameraConfigId;

    /** 绑定的摄像头业务编码，如 CAM-001 */
    private String cameraCode;

    /** 绑定的摄像头安装位置，如「西侧围墙中段」 */
    private String cameraName;

    /** 视频平台侧标识，非敏感 */
    private String cameraId;

    /** ONLINE / OFFLINE / UNKNOWN */
    private String deviceStatus;

    /** NORMAL / ACTIVE / RECOVERED */
    private String alarmStatus;

    /** ARMED / DISARMED，人工布防开关 */
    private String defenseStatus;

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
