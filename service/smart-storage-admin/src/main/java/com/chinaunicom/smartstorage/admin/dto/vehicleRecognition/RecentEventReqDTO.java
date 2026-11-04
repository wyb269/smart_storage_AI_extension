package com.chinaunicom.smartstorage.admin.dto.vehicleRecognition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 首页最近事件请求。
 *
 * <p>{@code limit} 可缺省，服务端会按常量兜底并限制上限，
 * 防止被滥用为全表扫描。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Data
@Schema(description = "最近事件查询请求")
public class RecentEventReqDTO {

    @Schema(description = "展示条数，缺省取服务端默认值")
    @Min(value = 1, message = "展示条数至少为 1")
    @Max(value = 50, message = "展示条数最多为 50")
    private Integer limit;
}
