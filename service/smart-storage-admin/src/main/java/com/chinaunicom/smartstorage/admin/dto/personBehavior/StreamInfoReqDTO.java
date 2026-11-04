package com.chinaunicom.smartstorage.admin.dto.personBehavior;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 播放信息请求。
 *
 * <p>入参是<b>监测区域标识</b>而不是摄像头标识：本功能的监测对象是区域，
 * 摄像头只是区域绑定的属性，前端网格上点的是区域卡片。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
@Data
@Schema(description = "播放信息请求")
public class StreamInfoReqDTO {

    @Schema(description = "监测区域标识")
    @NotBlank(message = "监测区域标识不能为空")
    @Size(max = 64, message = "监测区域标识过长")
    private String pointId;
}
