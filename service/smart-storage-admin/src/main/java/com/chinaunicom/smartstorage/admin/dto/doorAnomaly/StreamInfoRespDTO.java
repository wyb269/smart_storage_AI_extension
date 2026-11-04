package com.chinaunicom.smartstorage.admin.dto.doorAnomaly;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 视频播放信息响应对象。
 *
 * <p>字段严格对齐 AI_HANDOFF.md 第 20.2 节「视频播放信息」。</p>
 *
 * <p><b>安全约束</b>：本对象<b>严禁</b>新增摄像头用户名、密码、
 * 永久 Token 或含明文凭据的 RTSP 地址字段（第 20.2、29.1 节）。
 * 播放地址必须为短时签名地址（有效期 2 到 10 分钟）。</p>
 *
 * @author 仓门异动功能
 */
@Data
@Schema(description = "视频播放信息")
public class StreamInfoRespDTO {

    @Schema(description = "对应仓门")
    private String doorId;

    @Schema(description = "对应摄像头")
    private String cameraId;

    @Schema(description = "浏览器可播放的短时地址")
    private String playableUrl;

    @Schema(description = "播放协议 HLS/FLV/WEBRTC/MP4")
    private String protocol;

    @Schema(description = "地址失效时间")
    private String expiresAt;

    @Schema(description = "当前用户是否有权限播放")
    private Boolean canPlay;

    @Schema(description = "不可播放原因")
    private String unavailableReason;
}
