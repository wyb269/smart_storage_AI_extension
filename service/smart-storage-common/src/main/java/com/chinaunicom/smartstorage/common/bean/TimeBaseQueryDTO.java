package com.chinaunicom.smartstorage.common.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 条件查询基础类
 *
 * @author liuchink
 * @since 2017/12/18 10:57
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class TimeBaseQueryDTO extends BaseQueryDTO {
    @Schema(description = "开始时间", nullable = true, example = "12345")
    private LocalDateTime startTime;
    @Schema(description = "结束时间", nullable = true, example = "12345")
    private LocalDateTime endTime;

}
