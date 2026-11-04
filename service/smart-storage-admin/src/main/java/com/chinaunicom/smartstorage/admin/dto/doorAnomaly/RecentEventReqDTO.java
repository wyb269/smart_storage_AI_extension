package com.chinaunicom.smartstorage.admin.dto.doorAnomaly;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 最近异动事件查询请求。
 *
 * <p><b>契约统一说明</b>：与 {@link StreamInfoReqDTO} 同理，
 * 把此前 {@code @RequestParam int limit} 改为显式 {@code @RequestBody}，
 * 使前后端参数契约唯一。</p>
 *
 * <p><b>约束</b>：limit 取值 1~50，超出范围由 Bean Validation 直接拒绝，
 * 给出可理解的业务错误，而不是静默截断（第 16.5 节首页建议 5 条）。</p>
 *
 * @author 仓门异动功能
 */
@Data
@Schema(description = "最近异动事件查询请求")
public class RecentEventReqDTO {

    @Min(value = 1, message = "展示条数不能小于1")
    @Max(value = 50, message = "展示条数不能大于50")
    @Schema(description = "展示条数，首页建议5", defaultValue = "5")
    private Integer limit = 5;
}