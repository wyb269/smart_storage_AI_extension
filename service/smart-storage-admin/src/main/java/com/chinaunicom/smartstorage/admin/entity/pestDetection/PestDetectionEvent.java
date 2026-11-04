package com.chinaunicom.smartstorage.admin.entity.pestDetection;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 粮面虫害采样检测事件实体，对应表 {@code ai_pest_event}。
 *
 * <p>事件是一次<b>采样检测结果</b>：算法任务状态、算法结论、人工处理状态三者独立。
 * 人工处理<b>不得</b>覆盖或删除算法结果。</p>
 *
 * <p><b>密度判定</b>：{@code density > density_threshold} 即判密度超标告警。
 * {@code density_threshold} 存的是该条采样当时生效的阈值快照；
 * 缺省阈值只来自 {@code PestDetectionConstant.DEFAULT_DENSITY_THRESHOLD}，
 * 不得写死在 SQL 或页面里。</p>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
@Data
@TableName("ai_pest_event")
public class PestDetectionEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 采样编号，前缀 PT */
    private String eventNo;

    /** 关联粮面监测点位 */
    private Long pointId;

    /** 设备侧摄像头标识快照 */
    private String cameraId;

    /** 摄像头业务编码快照，如 CAM-003 */
    private String cameraCode;

    /** 采样位置，如 东墙地上层 / 中墙下层 / 西北墙中层 */
    private String samplePosition;

    /** 所属仓库，数据范围强制过滤维度 */
    private Long warehouseId;

    private String warehouseName;

    /** 粮种快照 */
    private String grainType;

    private String regionId;

    private Long orgId;

    /** GRAIN_INSECT / DENSITY_HIGH / MOLD / OTHER */
    private String conclusionType;

    private String conclusionTypeName;

    /** 虫种，如 玉米象/赤拟谷盗；无虫时为空 */
    private String insectSpecies;

    /** 密度（头/kg），非负 */
    private BigDecimal density;

    /** 判定阈值（头/kg），非负；density 超过它即判密度超标 */
    private BigDecimal densityThreshold;

    private String eventDesc;

    /** 采样时间，列表默认按此倒序 */
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
