package com.chinaunicom.smartstorage.admin.entity.doorAnomaly;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 异动事件实体。
 *
 * <p>对应表 {@code ai_anomaly_event}（逻辑模型 27.3）。</p>
 *
 * <p><b>三类状态相互独立（第 19 节、第 23 节）</b>：</p>
 * <ul>
 *   <li>{@code algorithmStatus} 算法任务状态</li>
 *   <li>{@code algorithmResult} 算法结论（未产出时为 null，<b>不得伪造为 NORMAL</b>）</li>
 *   <li>{@code processStatus} 人工处理状态</li>
 * </ul>
 *
 * @author 仓门异动功能
 */
@Data
@TableName("ai_anomaly_event")
public class AiAnomalyEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，同时作为对外事件标识 */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 对外事件编号 */
    private String eventNo;

    private Long doorId;

    private String doorName;

    private Long warehouseId;

    private String warehouseName;

    private String cameraId;

    private String cameraCode;

    /** 行政区划码，支撑数据范围过滤 */
    private String regionId;

    private Long orgId;

    /** 异动类型编码 */
    private String anomalyType;

    /** 异动类型名称快照 */
    private String anomalyTypeName;

    private String eventDesc;

    /** 异动发生时间，列表默认按此倒序 */
    private LocalDateTime occurredAt;

    private LocalDateTime recoveredAt;

    /** 关联算法任务标识 */
    private Long algorithmTaskId;

    /** 算法任务状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK */
    private String algorithmStatus;

    /** 算法结论 NORMAL/ANOMALY/UNCERTAIN；未产出结论时为 null */
    private String algorithmResult;

    /** 置信度 0-1，可为空。空表示「暂无」，与数值 0 含义不同 */
    private BigDecimal confidence;

    /** 人工处理状态 PENDING/PROCESSING/VERIFIED_TRUE/VERIFIED_FALSE/CLOSED */
    private String processStatus;

    /** 最近处理人，由后端按登录态写入 */
    private String processedBy;

    private LocalDateTime processedAt;

    /** 最近处理意见摘要；完整历史在 ai_event_process_record */
    private String processOpinion;

    /** 乐观锁版本号，用于并发覆盖控制 */
    private Integer eventVersion;

    /** 是否模拟数据（第 39.6 节） */
    private Boolean isSimulated;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;
}