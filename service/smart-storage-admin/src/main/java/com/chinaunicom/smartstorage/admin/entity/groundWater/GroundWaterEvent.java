package com.chinaunicom.smartstorage.admin.entity.groundWater;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积水事件实体，对应表 {@code ai_water_event}。
 *
 * <p>算法任务状态、算法结论、人工处理状态三者独立。
 * 人工处理<b>不得</b>覆盖或删除算法结果。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
@Data
@TableName("ai_water_event")
public class GroundWaterEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 事件编号，前缀 WP */
    private String eventNo;

    /** 关联监测点位 */
    private Long pointId;

    /** 点位名称快照，列表「监测位置」列直接展示，不随配置变更改写 */
    private String pointName;

    /** 视频平台侧摄像头标识快照，与 ai_water_monitor_point.camera_id 同源 */
    private String cameraId;

    private String cameraCode;

    /** 所属仓库，数据范围强制过滤维度 */
    private Long warehouseId;

    private String warehouseName;

    private String regionId;

    private Long orgId;

    /** PONDING / LEAK / ICE / OTHER */
    private String waterType;

    private String waterTypeName;

    private String eventDesc;

    /** 发生时间，列表默认按此倒序 */
    private LocalDateTime occurredAt;

    private LocalDateTime recoveredAt;

    /** 积水持续时长（秒） */
    private Long durationSeconds;

    private Long algorithmTaskId;

    private String algorithmStatus;

    /** NORMAL / ANOMALY / UNCERTAIN；未产出为 null */
    private String algorithmResult;

    /** 置信度 0-1，null 表示「暂无」，与数值 0 含义不同 */
    private BigDecimal confidence;

    /** PENDING / PROCESSING / DONE */
    private String processStatus;

    /** 最近处理人，由后端按登录态写入 */
    private String processedBy;

    private LocalDateTime processedAt;

    private String processOpinion;

    /** 乐观锁版本号 */
    private Integer eventVersion;

    /** 是否模拟数据 */
    private Boolean isSimulated;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;
}
