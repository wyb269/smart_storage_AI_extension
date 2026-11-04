package com.chinaunicom.smartstorage.admin.dto.aggregation.minio;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MinioCredentialResponseDTO {



    @Schema(description = "accessKey")
    private String accessKey;

    @Schema(description = "secretKey")
    private String secretKey;

    @Schema(description = "sessionToken")
    private String sessionToken;

    @Schema(description = "是否过期")
    private Boolean isExpired;

    @Schema(description = "过期时间")
    private String expirationDate;
}
