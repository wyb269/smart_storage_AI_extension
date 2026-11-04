package com.chinaunicom.smartstorage.applet.entity.aggregation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liuchink
 * @since 2024/07/11 13:13
 */
@Data
public class MinioConfigurationRespDTO {
    /**
     * Minio 连接地址
     */
    @Schema(description = "endpoint", nullable = true, example = "12345")
    private String endpoint;

    /**
     * accessKey 或 账号
     */
    @Schema(description = "accessKey", nullable = true, example = "12345")
    private String accessKey;

    /**
     * secretKey 或 密码
     */
    @Schema(description = "secretKey", nullable = true, example = "12345")
    private String secretKey;

    /**
     * 桶名称
     */
    @Schema(description = "bucketName", nullable = true, example = "12345")
    private String bucketName;
}
