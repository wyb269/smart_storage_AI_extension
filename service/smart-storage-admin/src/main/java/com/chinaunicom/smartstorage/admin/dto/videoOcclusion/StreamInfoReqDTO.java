package com.chinaunicom.smartstorage.admin.dto.videoOcclusion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 播放信息请求。
 *
 * @author aiVision 功能族 · 视频遮挡检测
 */
@Data
@Schema(description = "播放信息请求")
public class StreamInfoReqDTO {

    @Schema(description = "摄像头配置标识")
    @NotBlank(message = "摄像头标识不能为空")
    @Size(max = 64, message = "摄像头标识过长")
    private String cameraId;
}
