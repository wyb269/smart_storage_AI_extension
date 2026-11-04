package com.chinaunicom.smartstorage.admin.dto.groundWater;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 播放信息响应。
 *
 * <p><b>禁止</b>承载摄像头账号、密码、永久 Token 或含明文凭据的 RTSP 地址。
 * {@code playableUrl} 必须是短时签名地址，过期后前端重新调用。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
@Data
@Schema(description = "播放信息")
public class StreamInfoRespDTO {

    @Schema(description = "监测点位标识")
    private String pointId;

    @Schema(description = "点位名称，右侧监控区标题用")
    private String pointName;

    @Schema(description = "摄像头编号")
    private String cameraCode;

    @Schema(description = "视频平台侧标识")
    private String platformCameraId;

    @Schema(description = "浏览器可播放的短时地址")
    private String playableUrl;

    @Schema(description = "视频协议 HLS/FLV/WEBRTC/MP4")
    private String protocol;

    @Schema(description = "地址失效时间")
    private String expiresAt;

    @Schema(description = "是否可播放；缺省视为不可播放")
    private Boolean canPlay;

    @Schema(description = "不可播放原因，canPlay 为 false 时必填")
    private String unavailableReason;
}
