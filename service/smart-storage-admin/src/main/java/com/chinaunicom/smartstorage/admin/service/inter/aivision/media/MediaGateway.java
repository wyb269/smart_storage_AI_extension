package com.chinaunicom.smartstorage.admin.service.inter.aivision.media;

/**
 * 媒体网关。
 *
 * <p>依据 AI_HANDOFF.md 第 15.2、20.2、26.2、29.1 节：</p>
 * <ul>
 *   <li>前端<b>不得直接访问</b>带有摄像头账号、密码或永久 Token 的 RTSP 地址。</li>
 *   <li>播放地址与证据图片均通过<b>短时签名</b>访问，建议有效期 2 到 10 分钟。</li>
 *   <li>地址过期后由前端重新申请。</li>
 *   <li>后端必须同时校验用户权限与仓库数据权限。</li>
 *   <li>日志不得完整打印播放地址。</li>
 * </ul>
 *
 * <p><b>实现说明</b>：本期提供两个实现——</p>
 * <ul>
 *   <li>{@code S3MediaGateway}：复用项目已有的 {@code S3Presigner} 能力，
 *       用于真实对象存储证据图签发；</li>
 *   <li>{@code MockMediaGateway}：视频协议未确认前（第 37 节），
 *       仅返回占位状态与不可播放原因，不返回任何真实地址。</li>
 * </ul>
 *
 * @author 仓门异动功能
 */
public interface MediaGateway {

    /**
     * 签发视频播放短时地址。
     *
     * @param request 播放请求
     * @return 播放信息；不可播放时 {@code canPlay} 为 false 并给出原因
     */
    PlaybackGrant signPlayback(PlaybackRequest request);

    /**
     * 签发证据图片短时地址。
     *
     * @param objectKey 对象存储键
     * @param expiresMinutes 有效期（分钟）
     * @return 短时访问地址
     */
    EvidenceGrant signEvidence(String objectKey, int expiresMinutes);

    /**
     * 播放请求。
     */
    record PlaybackRequest(
        String subjectId,
        String cameraId,
        String warehouseId,
        /** 视频平台设备号（非敏感标识） */
        String deviceCode,
        /** 视频平台通道号 */
        String channelNo,
        /** 登录用户标识，用于校验权限与限制地址使用范围 */
        String userId
    ) {
    }

    /**
     * 播放授权结果。
     *
     * @param canPlay 是否可播放
     * @param playableUrl 浏览器可播放的短时地址；不可播放时为 null
     * @param protocol 播放协议；协议未确认前返回 MP4
     * @param expiresAt 失效时间
     * @param unavailableReason 不可播放原因
     */
    record PlaybackGrant(
        boolean canPlay,
        String playableUrl,
        String protocol,
        String expiresAt,
        String unavailableReason
    ) {
    }

    /**
     * 证据授权结果。
     *
     * @param url 短时访问地址
     * @param expiresAt 失效时间
     */
    record EvidenceGrant(String url, String expiresAt) {
    }
}