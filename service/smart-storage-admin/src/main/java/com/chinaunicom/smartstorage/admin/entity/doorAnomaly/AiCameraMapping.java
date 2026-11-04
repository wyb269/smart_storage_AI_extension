package com.chinaunicom.smartstorage.admin.entity.doorAnomaly;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 摄像头映射实体。
 *
 * <p>对应表 {@code ai_camera_mapping}（逻辑模型 27.2）。</p>
 *
 * <p><b>安全约束（第 20.2、29.1 节）</b>：本表<b>严禁</b>新增
 * 摄像头账号、密码、RTSP 凭据、永久 Token 等敏感列。
 * 视频平台凭据由网关侧持有，本表仅存非敏感标识。</p>
 *
 * @author 仓门异动功能
 */
@Data
@TableName("ai_camera_mapping")
public class AiCameraMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long doorId;

    /** 摄像头标识 */
    private String cameraId;

    /** 展示编号 */
    private String cameraCode;

    private String cameraName;

    /** 视频平台设备号（非敏感标识） */
    private String deviceCode;

    /** 视频平台通道号 */
    private String channelNo;

    /** 支持协议，逗号分隔 */
    private String supportedProtocols;

    private Boolean enabled;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;
}