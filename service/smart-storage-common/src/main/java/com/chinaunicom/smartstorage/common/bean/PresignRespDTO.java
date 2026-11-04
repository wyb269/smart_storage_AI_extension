package com.chinaunicom.smartstorage.common.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PresignRespDTO {


    @Schema(description = "预签名URL")
    private String url;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "对象")
    private String objectKey;

}
