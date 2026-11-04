package com.chinaunicom.smartstorage.admin.dto.task.qualityItem;

import com.chinaunicom.smartstorage.common.bean.BaseQueryDTO;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
/**
 * @Description  质量检验
 * @author zhangy
 * @Date 2026-01-29
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class QualityItemQueryReqDTO extends BaseQueryDTO {
    @Hidden
    private String startTime;

    @Hidden
    private String endTime;
}
