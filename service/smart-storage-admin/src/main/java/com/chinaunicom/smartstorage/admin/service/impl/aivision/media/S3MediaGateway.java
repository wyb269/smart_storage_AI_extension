package com.chinaunicom.smartstorage.admin.service.impl.aivision.media;

import com.chinaunicom.smartstorage.admin.config.RustFSConfiguration;
import com.chinaunicom.smartstorage.admin.constant.DoorAnomalyConstant;
import com.chinaunicom.smartstorage.admin.service.inter.aivision.media.MediaGateway;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 基于对象存储的媒体网关。
 *
 * <p>复用项目已有的 {@link S3Presigner} 能力
 * （与既有 {@code S3Controller} 同一套签发机制），不新建签名体系。</p>
 *
 * <p><b>职责边界</b>：</p>
 * <ol>
 *   <li>仅负责<b>证据图片</b>的短时地址签发。</li>
 *   <li>视频播放地址由视频平台网关提供，本类不处理
 *       （视频协议与平台尚未确认，第 37 节）。</li>
 *   <li>有效期上限收敛到 {@link DoorAnomalyConstant#EVIDENCE_URL_EXPIRE_MINUTES}，
 *       满足第 26.2 节「2 到 10 分钟」的要求。</li>
 * </ol>
 *
 * <p><b>安全要求</b>：</p>
 * <ul>
 *   <li>只接受受控对象存储键，拒绝任意 URL，防止越权读取。</li>
 *   <li>日志不打印完整签名地址。</li>
 * </ul>
 *
 * @author 仓门异动功能
 */
@Slf4j
@Component
@Primary
public class S3MediaGateway implements MediaGateway {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private RustFSConfiguration rustFSConfiguration;

    @Resource
    private S3Presigner s3Presigner;

    @Override
    public PlaybackGrant signPlayback(PlaybackRequest request) {
        // 视频播放地址不由对象存储提供，交由视频平台网关签发。
        // 视频协议与平台尚未确认（第37节），此处明确返回不可播放而非猜测地址。
        if (request == null || request.subjectId() == null || request.subjectId().isBlank()) {
            return new PlaybackGrant(false, null, "MP4", null, "DOOR_NOT_FOUND");
        }
        if (request.cameraId() == null || request.cameraId().isBlank()) {
            return new PlaybackGrant(false, null, "MP4", null, "CAMERA_NOT_BOUND");
        }

        // 不打印 deviceCode 与任何地址信息（第26.2、29.1节）
        log.info("[S3Media] 视频播放由视频平台网关处理，本网关不签发 subjectId={}", request.subjectId());
        return new PlaybackGrant(false, null, "MP4", null, "VIDEO_GATEWAY_NOT_READY");
    }

    @Override
    public EvidenceGrant signEvidence(String objectKey, int expiresMinutes) {
        if (objectKey == null || objectKey.isBlank()) {
            return new EvidenceGrant(null, null);
        }

        // 安全校验：禁止为任意 URL 或本地路径签发，防止越权读取与 SSRF
        if (objectKey.contains("..") || objectKey.contains("://") || objectKey.contains("\\")) {
            log.warn("[S3Media] 拒绝非法对象键：包含路径穿越或协议标识");
            return new EvidenceGrant(null, null);
        }

        final int minutes = Math.min(
            expiresMinutes <= 0 ? DoorAnomalyConstant.EVIDENCE_URL_EXPIRE_MINUTES : expiresMinutes,
            DoorAnomalyConstant.EVIDENCE_URL_EXPIRE_MINUTES);

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(rustFSConfiguration.getBucketName())
                .key(objectKey)
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(minutes))
                .getObjectRequest(getRequest)
                .build();

            String url = s3Presigner.presignGetObject(presignRequest).url().toString();
            // 仅记录对象键，不打印完整签名地址
            log.info("[S3Media] 已签发证据短时地址 objectKey={} 有效期={}分钟", objectKey, minutes);

            return new EvidenceGrant(url,
                LocalDateTime.now().plusMinutes(minutes).format(TS));
        } catch (Exception e) {
            log.error("[S3Media] 证据地址签发失败 objectKey={}", objectKey, e);
            return new EvidenceGrant(null, null);
        }
    }
}