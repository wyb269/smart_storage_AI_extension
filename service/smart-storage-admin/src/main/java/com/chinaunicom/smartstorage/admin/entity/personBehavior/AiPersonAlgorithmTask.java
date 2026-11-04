package com.chinaunicom.smartstorage.admin.entity.personBehavior;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 人员行为算法任务与结果实体。
 *
 * <p>对应表 {@code ai_person_algorithm_task}
 * （建表脚本 docs/person-behavior/sql/05_create_person_algorithm_task.sql）。
 * 形状镜像视频遮挡的 {@code ai_occlusion_algorithm_task}，主体维度由摄像头换为监测区域：
 * 功能内主体是 {@code pointId}（指向 {@code ai_person_monitor_point.id}），
 * {@code cameraId} / {@code cameraCode} 为区域内绑定摄像头的快照。</p>
 *
 * <p><b>写入路径</b>：「重新识别」（{@code /personBehavior/rerunAlgorithm/{eventId}}）
 * 受理后落一行 QUEUED 任务；轮询 {@code /personBehavior/algorithmTask/{taskId}} 时
 * 由适配层输出更新状态与结论，成功后回写事件的
 * {@code algorithm_task_id / algorithm_status / confidence / behavior_type}。
 * 历史演示数据见 docs/person-behavior/sql/07_seed_algorithm_tasks.sql。</p>
 *
 * <p><b>安全约束</b>：{@code mediaRefs}、{@code normalReferenceRef}、{@code rawResultRef}
 * 只能存受控内部引用，禁止存放任意 URL 或本地绝对路径，防止 SSRF 与路径穿越。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
@Data
@TableName("ai_person_algorithm_task")
public class AiPersonAlgorithmTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 算法任务编号 */
    private String taskNo;

    /** 产出了哪条行为事件；未落事件时为空 */
    private Long eventId;

    /** 监测区域标识，指向 ai_person_monitor_point.id，本表的功能内主体 */
    private Long pointId;

    /** 设备侧摄像头标识，与 ai_person_event.camera_id 同源 */
    private String cameraId;

    /** 摄像头业务编码，如 CAM-001 */
    private String cameraCode;

    /** 所属仓库，数据范围强制过滤维度 */
    private Long warehouseId;

    /** 行政区划码，数据范围下推用；查询本表必须带范围条件 */
    private String regionId;

    private Long orgId;

    /** 任务类型，人员行为识别固定 PERSON_BEHAVIOR_DETECT */
    private String taskType;

    /** SINGLE_IMAGE/DUAL_IMAGE/FRAME_SEQUENCE/VIDEO */
    private String mediaType;

    /** 受控内部媒体引用（JSON 数组），禁止任意 URL */
    private String mediaRefs;

    /** 正常参考资源引用 */
    private String normalReferenceRef;

    /** 人员行为检测区域（JSON） */
    private String roi;

    /** 执行状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK */
    private String executionStatus;

    /** 结论 NORMAL/ANOMALY/UNCERTAIN */
    private String conclusion;

    /** 标准化行为类型，取值同 ai_person_event.behavior_type */
    private String behaviorType;

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

    /**
     * 检出目标数。
     *
     * <p><b>不是表列</b>：查询时由 detections JSON 的数组长度派生
     * （见 PersonAlgorithmTaskMapper.xml）；内存实现与写库前直接赋值。</p>
     */
    @TableField(exist = false)
    private Integer detectedCount;

    /** 证据引用（JSON） */
    private String evidenceRefs;

    /** 原始算法输出受控引用，前端不解析 */
    private String rawResultRef;

    private String errorCode;

    private String errorMessage;

    /** 重试次数，默认 0 */
    private Integer retryCount = 0;

    /** 全链路追踪编号 */
    private String traceId;

    /** 是否模拟结果，必须明确标记且默认为真 */
    private Boolean isSimulated = Boolean.TRUE;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;
}
