package com.chinaunicom.smartstorage.admin.dto.task.sampleItem;

import com.chinaunicom.smartstorage.common.bean.BaseQueryDTO;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
/**
 * @Description 扦样信息
 * @author zhangy
 * @Date 2026-01-26
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class SampleItemQueryReqDTO extends BaseQueryDTO {
    @Hidden
    private String startTime;

    @Hidden
    private String endTime;
}
