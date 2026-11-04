package com.chinaunicom.smartstorage.admin.dto.beltMachine;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 播放信息请求。
 *
 * <p>入参是<b>皮带机点位标识</b>而不是摄像头标识：本功能的监测对象是皮带机，
 * 前端网格只有点位概念，由后端解析该点位绑定的摄像头再取流。</p>
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
@Data
@Schema(description = "皮带机点位播放信息请求")
public class BeltMachineStreamInfoReqDTO {

    @Schema(description = "监测点位标识")
    @NotBlank(message = "监测点位标识不能为空")
    @Size(max = 64, message = "监测点位标识过长")
    private String pointId;
}
