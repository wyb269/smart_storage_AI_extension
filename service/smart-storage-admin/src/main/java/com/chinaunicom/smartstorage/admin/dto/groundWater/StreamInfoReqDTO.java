package com.chinaunicom.smartstorage.admin.dto.groundWater;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 播放信息请求。
 *
 * <p>本功能的取流主体是<b>监测点位</b>，因此入参是 pointId；
 * 点位到摄像头的绑定关系由后端解析，前端不感知。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
@Data
@Schema(description = "播放信息请求")
public class StreamInfoReqDTO {

    @Schema(description = "监测点位标识")
    @NotBlank(message = "监测点位标识不能为空")
    @Size(max = 64, message = "监测点位标识过长")
    private String pointId;
}
