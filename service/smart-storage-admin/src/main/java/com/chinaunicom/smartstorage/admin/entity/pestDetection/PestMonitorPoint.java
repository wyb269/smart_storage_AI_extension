package com.chinaunicom.smartstorage.admin.entity.pestDetection;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 粮面监测点位实体，对应表 {@code ai_pest_monitor_point}。
 *
 * <p><b>本功能的监测对象不是摄像头，而是「仓房 + 粮种 + 吨位」的粮面点位</b>：
 * 一个点位绑定一路粮面摄像头（{@code camera_id}/{@code camera_code}），
 * 卡片标题形如「三号仓 · 小麦5000吨」。</p>
 *
 * <p>设备连接状态与告警状态相互独立，页面展示状态由前端综合计算。</p>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
@Data
@TableName("ai_pest_monitor_point")
public class PestMonitorPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，同时作为对外点位标识 */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 点位名称，如「三号仓粮面」 */
    private String pointName;

    /** 所属仓库，数据范围强制过滤维度 */
    private Long warehouseId;

    private String warehouseName;

    /** 粮种，如 小麦/玉米/稻谷 */
    private String grainType;

    /** 存粮吨位，卡片标题拼接用 */
    private BigDecimal grainTonnage;

    /** 行政区划码 */
    private String regionId;

    private Long orgId;

    /** 绑定的粮面摄像头：视频平台侧标识，非敏感 */
    private String cameraId;

    /** 绑定的粮面摄像头：业务编码，如 CAM-003 */
    private String cameraCode;

    /** ONLINE / OFFLINE / UNKNOWN */
    private String deviceStatus;

    /** NORMAL / ACTIVE / RECOVERED（ACTIVE = 页面「告警」） */
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
