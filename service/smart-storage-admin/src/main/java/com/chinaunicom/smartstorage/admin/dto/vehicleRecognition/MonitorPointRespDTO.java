package com.chinaunicom.smartstorage.admin.dto.vehicleRecognition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 识别点位状态响应。
 *
 * <p>车辆功能左侧展示的是「今日识别车辆」而非点位状态网格，因此本对象<b>不作为独立接口返回</b>，
 * 仅用于 Service 内部按点位定位摄像头、判定在线，以及为后续点位健康视图预留形状。</p>
 *
 * <p>页面展示的点位综合态由前端综合 {@code deviceStatus} 与 {@code alarmStatus} 与心跳新鲜度算出，
 * 后端不返回合并后的单一字符串。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Data
@Schema(description = "识别点位状态")
public class MonitorPointRespDTO {

    @Schema(description = "点位标识")
    private String pointId;

    @Schema(description = "点位业务编码，如 GATE-E-IN")
    private String pointCode;

    @Schema(description = "点位名称，如「东大门入口」")
    private String pointName;

    @Schema(description = "所属仓库标识")
    private String warehouseId;

    @Schema(description = "所属仓库名称")
    private String warehouseName;

    @Schema(description = "绑定的摄像头业务编码")
    private String cameraCode;

    @Schema(description = "摄像头安装位置名称")
    private String cameraName;

    @Schema(description = "视频平台侧标识，非敏感")
    private String platformCameraId;

    @Schema(description = "设备连接状态 ONLINE/OFFLINE/UNKNOWN")
    private String deviceStatus;

    @Schema(description = "点位告警状态 NORMAL/ACTIVE/RECOVERED")
    private String alarmStatus;

    @Schema(description = "最近心跳时间")
    private String lastHeartbeatAt;

    @Schema(description = "最近识别时间")
    private String latestEventAt;

    @Schema(description = "展示顺序")
    private Integer sortNo;
}
