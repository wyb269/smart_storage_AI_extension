package com.chinaunicom.smartstorage.admin.dto.beltMachine;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 播放信息响应。
 *
 * <p><b>禁止</b>承载摄像头账号、密码、永久 Token 或含明文凭据的 RTSP 地址。
 * {@code playableUrl} 必须是短时签名地址，过期后前端重新调用。</p>
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
@Data
@Schema(description = "皮带机点位播放信息")
public class BeltMachineStreamInfoRespDTO {

    @Schema(description = "监测点位标识")
    private String pointId;

    @Schema(description = "视频平台侧摄像头标识")
    private String cameraId;

    @Schema(description = "摄像头业务编码，用于右侧标题")
    private String cameraCode;

    @Schema(description = "摄像头位置名称，用于右侧标题")
    private String cameraName;

    @Schema(description = "浏览器可播放的短时地址")
    private String playableUrl;

    @Schema(description = "视频协议 HLS/FLV/WEBRTC/MP4")
    private String protocol;

    @Schema(description = "地址失效时间")
    private String expiresAt;

    @Schema(description = "是否可播放；缺省视为不可播放")
    private Boolean canPlay;

    /**
     * 不可播放原因。
     *
     * <p>本功能比视频遮挡多一个 {@code CAMERA_NOT_BOUND}：
     * 皮带机台账存在但没绑摄像头（页面「未部署」），
     * 这既不是掉线也不是无权限，混进 {@code CAMERA_OFFLINE}
     * 会让运维去查网络，而实际要做的是装摄像头。</p>
     */
    @Schema(description = "不可播放原因，canPlay 为 false 时必填")
    private String unavailableReason;
}
