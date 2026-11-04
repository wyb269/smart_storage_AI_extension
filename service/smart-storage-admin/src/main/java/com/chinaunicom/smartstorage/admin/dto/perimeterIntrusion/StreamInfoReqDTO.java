package com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 播放信息请求。
 *
 * <p>入参是<b>防区段标识</b>而不是摄像头标识：一个防区段绑定一路摄像头，
 * 由后端解析绑定关系后再向媒体网关取流，前端不需要知道摄像头主键。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
@Data
@Schema(description = "播放信息请求")
public class StreamInfoReqDTO {

    @Schema(description = "防区段标识")
    @NotBlank(message = "防区段标识不能为空")
    @Size(max = 64, message = "防区段标识过长")
    private String pointId;
}
