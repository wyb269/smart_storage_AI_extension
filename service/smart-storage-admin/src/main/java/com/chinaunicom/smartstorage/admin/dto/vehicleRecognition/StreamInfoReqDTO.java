package com.chinaunicom.smartstorage.admin.dto.vehicleRecognition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 播放信息请求。
 *
 * <p>车辆功能取流的主体是「识别记录所属点位的摄像头」，因此入参是点位标识；
 * 前端选中一条今日识别记录后，用该记录的 pointId 请求播放地址。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Data
@Schema(description = "播放信息请求")
public class StreamInfoReqDTO {

    @Schema(description = "识别点位标识")
    @NotBlank(message = "点位标识不能为空")
    @Size(max = 64, message = "点位标识过长")
    private String pointId;
}
