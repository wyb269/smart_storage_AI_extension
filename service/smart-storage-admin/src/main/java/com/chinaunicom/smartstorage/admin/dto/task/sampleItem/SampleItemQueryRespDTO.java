package com.chinaunicom.smartstorage.admin.dto.task.sampleItem;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * @Description 扦样信息
 * @author zhangy
 * @Date 2026-01-26
 */
@Data
public class SampleItemQueryRespDTO {
    @Schema(name ="id", description = "唯一标识", nullable = true)
    private String id;
    @Schema(name ="cardNo", description = "智能卡编号", nullable = true)
    private String cardNo;
    @Schema(name ="sampleNo", description = "扦样单编号", nullable = true)
    private String sampleNo;
    @Schema(name ="grainVariety", description = "粮食品种", nullable = true)
    private String grainVariety;
    @Schema(name ="carrierName", description = "承运人", nullable = true)
    private String carrierName;
    @Schema(name ="licensePlate", description = "车牌号", nullable = true)
    private String licensePlate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(name ="addTime", description = "添加时间", nullable = true)
    private LocalDateTime addTime;
}
