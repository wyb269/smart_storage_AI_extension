package com.chinaunicom.smartstorage.admin.service.impl.aivision.media;

import com.chinaunicom.smartstorage.admin.constant.DoorAnomalyConstant;
import com.chinaunicom.smartstorage.admin.service.inter.aivision.media.MediaGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 模拟媒体网关。
 *
 * <p><b>使用场景</b>：视频协议尚未最终确认（第 37 节）、
 * 真实视频网关平台未接入时使用。</p>
 *
 * <p><b>严格约束</b>：</p>
 * <ol>
 *   <li>本实现<b>只返回不可播放状态与原因，绝不返回任何真实摄像头地址</b>。</li>
 *   <li>不返回摄像头账号、密码、RTSP 凭据或永久 Token（第 29.1 节）。</li>
 *   <li>不访问任何外部视频平台，不发起网络请求。</li>
 *   <li>协议字段固定返回 MP4，仅表示 Mock 阶段的占位协议，
 *       <b>不作为安装播放器依赖的依据</b>（第 22 节）。</li>
 * </ol>
 *
 * <p>待视频平台与协议确认后，由 {@link S3MediaGateway} 或新增的
 * 视频平台适配器替换本实现，上层调用方无需改动。</p>
 *
 * @author 仓门异动功能
 */
@Slf4j
@Component
@org.springframework.context.annotation.Profile("mock-media")
public class MockMediaGateway implements MediaGateway {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PlaybackGrant signPlayback(PlaybackRequest request) {
        if (request == null || request.subjectId() == null || request.subjectId().isBlank()) {
            return new PlaybackGrant(false, null, "MP4", null, "DOOR_NOT_FOUND");
        }

        // 摄像头未绑定
        if (request.cameraId() == null || request.cameraId().isBlank()) {
            return new PlaybackGrant(false, null, "MP4", null, "CAMERA_NOT_BOUND");
        }

        // 视频平台未接入：明确告知原因，不返回任何地址
        // 日志只记录仓门标识，不打印任何媒体地址（第26.2节）
        log.info("[MockMedia] 视频平台未接入，返回不可播放状态 doorId={} cameraId={}",
            request.subjectId(), request.cameraId());

        return new PlaybackGrant(
            false,
            null,
            "MP4",
            null,
            "VIDEO_GATEWAY_NOT_READY"
        );
    }

    @Override
    public EvidenceGrant signEvidence(String objectKey, int expiresMinutes) {
        if (objectKey == null || objectKey.isBlank()) {
            return new EvidenceGrant(null, null);
        }

        // 安全校验：拒绝任意 URL 与路径穿越，只接受受控引用（第29.3节）
        if (objectKey.contains("..") || objectKey.contains("\\")
                || objectKey.startsWith("/")
                || objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            log.warn("[MockMedia] 拒绝非法对象引用（疑似越权访问）");
            return new EvidenceGrant(null, null);
        }

        final int minutes = Math.min(
            expiresMinutes <= 0 ? DoorAnomalyConstant.EVIDENCE_URL_EXPIRE_MINUTES : expiresMinutes,
            DoorAnomalyConstant.EVIDENCE_URL_EXPIRE_MINUTES);

        final LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(minutes);
        // Mock 阶段不连接真实对象存储。
        // 返回带过期查询参数的短时形态标识，体现「短时地址」模型而非永久地址。
        return new EvidenceGrant(objectKey + "?Expires=" + expiresAt.format(TS), expiresAt.format(TS));
    }

    /**
     * 播放地址默认有效期（分钟）。
     */
    public int defaultExpireMinutes() {
        return DoorAnomalyConstant.STREAM_URL_EXPIRE_MINUTES;
    }

    /**
     * 保留 Duration 引用，便于后续视频网关接入时直接复用。
     */
    protected Duration defaultDuration() {
        return Duration.ofMinutes(DoorAnomalyConstant.STREAM_URL_EXPIRE_MINUTES);
    }
}