package com.chinaunicom.smartstorage.admin.entity.perimeterIntrusion;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 周界入侵事件实体，对应表 {@code ai_perimeter_event}。
 *
 * <p>算法任务状态、算法结论、人工处理状态三者独立。
 * 人工处理<b>不得</b>覆盖或删除算法结果。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
@Data
@TableName("ai_perimeter_event")
public class PerimeterIntrusionEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 事件编号，前缀 PD */
    private String eventNo;

    /** 关联防区段 */
    private Long pointId;

    /** 防区段名称快照，如「东防区-东墙段01」 */
    private String pointName;

    /** 所属防区 EAST/WEST/SOUTH/NORTH，历史查询的筛选维度 */
    private String zone;

    /** 防区展示名快照 */
    private String zoneName;

    /** 绑定摄像头配置标识快照 */
    private Long cameraConfigId;

    private String cameraId;

    private String cameraCode;

    /** 摄像头位置快照，列表直接展示，不随配置变更改写 */
    private String cameraPosition;

    /** 所属仓库，数据范围强制过滤维度 */
    private Long warehouseId;

    private String warehouseName;

    private String regionId;

    private Long orgId;

    /** CLIMB / CROSS / INTRUSION / LOITER / OTHER */
    private String intrusionType;

    private String intrusionTypeName;

    private String eventDesc;

    /** 发生时间，列表默认按此倒序 */
    private LocalDateTime occurredAt;

    private LocalDateTime recoveredAt;

    /** 持续时长（秒） */
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
