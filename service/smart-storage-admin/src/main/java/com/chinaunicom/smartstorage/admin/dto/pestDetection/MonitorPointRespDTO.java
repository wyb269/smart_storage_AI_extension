package com.chinaunicom.smartstorage.admin.dto.pestDetection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 粮面点位状态响应。
 *
 * <p>对应原型图左侧「各仓虫情状态」网格的一张卡片，标题形如「三号仓 · 小麦5000吨」。
 * 页面展示的「正常 / 告警」由前端综合 {@code deviceStatus} 与 {@code alarmStatus}
 * 与心跳新鲜度算出，后端不返回合并后的单一字符串。</p>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
@Data
@Schema(description = "粮面点位虫情状态")
public class MonitorPointRespDTO {

    @Schema(description = "点位标识")
    private String pointId;

    @Schema(description = "点位名称，如「三号仓粮面」")
    private String pointName;

    @Schema(description = "所属仓库标识")
    private String warehouseId;

    @Schema(description = "所属仓库名称")
    private String warehouseName;

    @Schema(description = "粮种，如 小麦/玉米/稻谷")
    private String grainType;

    @Schema(description = "存粮吨位，卡片标题拼接用")
    private BigDecimal grainTonnage;

    @Schema(description = "绑定的粮面摄像头业务编码，如 CAM-003")
    private String cameraCode;

    @Schema(description = "视频平台侧标识，非敏感")
    private String platformCameraId;

    @Schema(description = "设备连接状态 ONLINE/OFFLINE/UNKNOWN")
    private String deviceStatus;

    @Schema(description = "告警状态 NORMAL/ACTIVE/RECOVERED")
    private String alarmStatus;

    @Schema(description = "最近心跳时间")
    private String lastHeartbeatAt;

    @Schema(description = "最近检测事件时间")
    private String latestEventAt;

    @Schema(description = "展示顺序")
    private Integer sortNo;
}
