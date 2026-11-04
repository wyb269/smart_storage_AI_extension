package com.chinaunicom.smartstorage.admin.entity.doorAnomaly;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 事件证据实体。
 *
 * <p>对应表 {@code ai_event_evidence}（逻辑模型 27.5）。</p>
 *
 * <p><b>关键约束（第 17.4、39.6 节）</b>：</p>
 * <ul>
 *   <li>媒体文件<b>不入库</b>，仅存对象存储键与元数据。</li>
 *   <li>{@code objectKey} 与 {@code annotatedObjectKey} <b>必须分离</b>，
 *       算法标注不得覆盖原图。</li>
 * </ul>
 *
 * @author 仓门异动功能
 */
@Data
@TableName("ai_event_evidence")
public class AiEventEvidence implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long eventId;

    /** NORMAL_REFERENCE/ABNORMAL_KEY_FRAME/BEFORE_FRAME/RECOVERY_IMAGE/EVENT_VIDEO */
    private String evidenceType;

    /** 原始资源对象存储键（非完整 URL） */
    private String objectKey;

    /** 算法标注图对象键，与原始资源分离存储 */
    private String annotatedObjectKey;

    /** 缩略图对象键 */
    private String thumbnailObjectKey;

    /** 采集时间，每张证据必须展示 */
    private LocalDateTime capturedAt;

    private String cameraCode;

    /** 资源完整性摘要 */
    private String contentHash;

    /** 展示顺序 */
    private Integer sortNo;

    private String createdBy;

    private LocalDateTime createdAt;
}