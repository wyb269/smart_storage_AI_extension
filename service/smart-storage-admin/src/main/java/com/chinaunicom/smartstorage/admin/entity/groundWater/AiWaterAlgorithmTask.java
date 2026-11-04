package com.chinaunicom.smartstorage.admin.entity.groundWater;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 地面积水算法任务与结果实体。
 *
 * <p>对应表 {@code ai_water_algorithm_task}
 * （建表脚本 docs/ground-water/sql/05_create_water_algorithm_task.sql）。
 * 形状照抄视频遮挡的 {@code ai_occlusion_algorithm_task}，
 * 主体维度由摄像头换为监测点位：功能内主体是 {@code pointId}
 * （指向 {@code ai_water_monitor_point.id}），{@code cameraId} / {@code cameraCode}
 * 保留为该点位绑定摄像头的设备侧标识与业务编码。</p>
 *
 * <p><b>本期不写入</b>：与仓门、遮挡两侧现状一致，
 * 本实体只是适配层的落地形状，真实算法到达后由新增适配器写入并回填
 * {@code ai_water_event.algorithm_task_id}。</p>
 *
 * <p><b>安全约束（第 24.2、29.3 节）</b>：</p>
 * <ul>
 *   <li>{@code mediaRefs}、{@code normalReferenceRef} 只能存受控内部引用，
 *       禁止存放任意 URL 或本地绝对路径，防止 SSRF 与路径穿越。</li>
 *   <li>{@code rawResultRef} 为原始算法输出的受控引用，前端永不直接解析。</li>
 * </ul>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
@Data
@TableName("ai_water_algorithm_task")
public class AiWaterAlgorithmTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 算法任务编号 */
    private String taskNo;

    /** 产出了哪条积水事件；未落事件时为空 */
    private Long eventId;

    /** 监测点位标识，指向 ai_water_monitor_point.id，本表的功能内主体 */
    private Long pointId;

    /** 设备侧摄像头标识，与 ai_water_event.camera_id 同源 */
    private String cameraId;

    /** 摄像头业务编码，如 CAM-001 */
    private String cameraCode;

    /** 所属仓库，数据范围强制过滤维度 */
    private Long warehouseId;

    /** 行政区划码，数据范围下推用；查询本表必须带范围条件 */
    private String regionId;

    private Long orgId;

    /** 任务类型，地面积水识别固定 GROUND_WATER_DETECT */
    private String taskType;

    /** SINGLE_IMAGE/DUAL_IMAGE/FRAME_SEQUENCE/VIDEO */
    private String mediaType;

    /** 受控内部媒体引用（JSON 数组），禁止任意 URL */
    private String mediaRefs;

    /** 干燥参考资源引用 */
    private String normalReferenceRef;

    /** 积水检测区域（JSON） */
    private String roi;

    /** 执行状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK */
    private String executionStatus;

    /** 结论 NORMAL/ANOMALY/UNCERTAIN */
    private String conclusion;

    /** 标准化积水类型，取值同 ai_water_event.water_type */
    private String waterType;

    private BigDecimal confidence;

    private String modelName;

    private String modelVersion;

    private String algorithmVersion;

    private String configVersion;

    private BigDecimal confidenceThreshold;

    /** 输入资源摘要，幂等键组成部分 */
    private String mediaDigest;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Long inferenceDurationMs;

    /** 检测对象、区域与置信度（JSON） */
    private String detections;

    /** 证据引用（JSON） */
    private String evidenceRefs;

    /** 原始算法输出受控引用，前端不解析 */
    private String rawResultRef;

    private String errorCode;

    private String errorMessage;

    /** 重试次数（第 30 节有限重试），默认 0 */
    private Integer retryCount = 0;

    /** 全链路追踪编号 */
    private String traceId;

    /** 是否模拟结果，必须明确标记且默认为真（第 24.4 节） */
    private Boolean isSimulated = Boolean.TRUE;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;
}
