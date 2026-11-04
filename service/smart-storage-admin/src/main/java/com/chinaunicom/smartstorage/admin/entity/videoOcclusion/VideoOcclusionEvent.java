package com.chinaunicom.smartstorage.admin.entity.videoOcclusion;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 遮挡事件实体，对应表 {@code ai_occlusion_event}。
 *
 * <p>算法任务状态、算法结论、人工处理状态三者独立。
 * 人工处理<b>不得</b>覆盖或删除算法结果。</p>
 *
 * @author aiVision 功能族 · 视频遮挡检测
 */
@Data
@TableName("ai_occlusion_event")
public class VideoOcclusionEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 事件编号，前缀 VD */
    private String eventNo;

    /** 关联摄像头配置 */
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

    /** FULL / PARTIAL / PAINT / LENS_COVER / OTHER */
    private String occlusionType;

    private String occlusionTypeName;

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
