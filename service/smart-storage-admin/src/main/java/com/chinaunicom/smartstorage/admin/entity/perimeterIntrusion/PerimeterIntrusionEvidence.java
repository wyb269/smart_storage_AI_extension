package com.chinaunicom.smartstorage.admin.entity.perimeterIntrusion;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 周界入侵证据实体，对应表 {@code ai_perimeter_evidence}。
 *
 * <p>只存受控对象存储引用（{@code mock://} 或 {@code s3://}），
 * 不存任意 URL 或本地绝对路径。短时地址由 MediaGateway 在详情接口签发。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
@Data
@TableName("ai_perimeter_evidence")
public class PerimeterIntrusionEvidence implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long eventId;

    /** INTRUSION_FRAME / NORMAL_REFERENCE / RECOVERY_IMAGE / EVENT_VIDEO */
    private String evidenceType;

    /** 受控内部媒体引用 */
    private String originalRef;

    /** 算法标注图引用，与原始引用分离，不得互相覆盖 */
    private String annotatedRef;

    private String thumbnailRef;

    private LocalDateTime capturedAt;

    private String cameraCode;

    /** 资源完整性摘要 */
    private String contentHash;

    private Integer sortNo;

    private String createdBy;

    private LocalDateTime createdAt;
}
