package com.chinaunicom.smartstorage.admin.dto.doorAnomaly;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 仓门状态响应对象。
 *
 * <p>字段严格对齐 AI_HANDOFF.md 第 20.1 节「仓门状态对象」，
 * 不做增删，避免前后端契约漂移。</p>
 *
 * @author 仓门异动功能
 */
@Data
@Schema(description = "仓门状态")
public class DoorStatusRespDTO {

    @Schema(description = "仓门唯一标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String doorId;

    @Schema(description = "仓门名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String doorName;

    @Schema(description = "仓库标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String warehouseId;

    @Schema(description = "仓库名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String warehouseName;

    @Schema(description = "摄像头唯一标识")
    private String cameraId;

    @Schema(description = "摄像头展示编号")
    private String cameraCode;

    @Schema(description = "设备连接状态 ONLINE/OFFLINE/UNKNOWN",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceStatus;

    @Schema(description = "仓门物理状态 OPEN/CLOSED/UNKNOWN",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String doorPhysicalStatus;

    @Schema(description = "告警状态 NORMAL/ACTIVE/RECOVERED",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String alarmStatus;

    @Schema(description = "最近心跳时间")
    private String lastHeartbeatAt;

    @Schema(description = "最近异动时间")
    private String latestEventAt;

    @Schema(description = "展示顺序", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sortNo;
}
