package com.chinaunicom.smartstorage.admin.dto.pestDetection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 播放信息请求。
 *
 * <p>本功能取流的主体是粮面点位，入参为点位标识，后端据其绑定的粮面摄像头取流。</p>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
@Data
@Schema(description = "播放信息请求")
public class StreamInfoReqDTO {

    @Schema(description = "粮面点位标识")
    @NotBlank(message = "点位标识不能为空")
    @Size(max = 64, message = "点位标识过长")
    private String pointId;
}
