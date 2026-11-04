package com.chinaunicom.smartstorage.admin.entity.beltMachine;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 皮带机下站人事件实体，对应表 {@code ai_belt_event}。
 *
 * <p>算法任务状态、算法结论、人工处理状态三者独立。
 * 人工处理<b>不得</b>覆盖或删除算法结果。</p>
 *
 * <p>{@code deviceName} 与 {@code cameraCode} 都是<b>发生时刻的快照</b>，
 * 点位改名或换绑摄像头后历史事件不应跟着变，否则记录表会出现
 * 「一号皮带机」和「二号皮带机」互相错挂的脏数据。</p>
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
@Data
@TableName("ai_belt_event")
public class BeltMachineEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 事件编号，前缀 BC */
    private String eventNo;

    /** 关联皮带机监测点位，指向 ai_belt_monitor_point.id */
    private Long pointId;

    private String deviceCode;

    /** 皮带机名称快照，列表「皮带机」列直接展示 */
    private String deviceName;

    /** 视频平台侧摄像头标识快照 */
    private String cameraId;

    /** 摄像头业务编码快照，列表「摄像头」列直接展示 */
    private String cameraCode;

    /** 所属仓库，数据范围强制过滤维度 */
    private Long warehouseId;

    private String warehouseName;

    private String regionId;

    private Long orgId;

    /** STANDING_UNDER / CROSSING / LOITERING / OTHER */
    private String conclusionType;

    private String conclusionTypeName;

    private String eventDesc;

    /** 画面内检出的人数；算法未给出时为 null，不得伪造为 0 */
    private Integer personCount;

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
