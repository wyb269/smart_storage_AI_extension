package com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 防区段状态响应。
 *
 * <p>对应原型图左侧「四面防区状态」网格的一张卡片，卡片标题形如「东防区-东墙段01」。
 * 页面展示的「正常布防 / 入侵报警 / 未布防 / 离线」由前端综合
 * {@code deviceStatus}、{@code alarmStatus}、心跳新鲜度与 {@code defenseStatus} 算出，
 * 后端不返回合并后的单一字符串。</p>
 *
 * <p><b>为什么要把 defenseStatus 单独返回</b>：共享层的 computeDisplayStatus
 * 只认「设备 + 告警 + 心跳」三样，表达不出人工撤防这一维；
 * 若后端预先折叠成一个字符串，前端就无法同时呈现「未布防」与「离线」两种信息。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
@Data
@Schema(description = "防区段入侵状态")
public class MonitorPointStatusRespDTO {

    @Schema(description = "防区段标识")
    private String pointId;

    @Schema(description = "防区段编码，如 PD-001")
    private String pointCode;

    @Schema(description = "防区段名称，如 东防区-东墙段01")
    private String pointName;

    @Schema(description = "所属防区 EAST/WEST/SOUTH/NORTH")
    private String zone;

    @Schema(description = "防区展示名，如 东区")
    private String zoneName;

    @Schema(description = "墙段名称，如 东墙段01")
    private String segmentName;

    @Schema(description = "所属仓库标识")
    private String warehouseId;

    @Schema(description = "所属仓库名称")
    private String warehouseName;

    @Schema(description = "绑定摄像头配置标识")
    private String cameraConfigId;

    @Schema(description = "绑定摄像头编号，如 CAM-001")
    private String cameraCode;

    @Schema(description = "绑定摄像头位置名称")
    private String cameraName;

    @Schema(description = "视频平台侧标识，非敏感")
    private String platformCameraId;

    @Schema(description = "设备连接状态 ONLINE/OFFLINE/UNKNOWN")
    private String deviceStatus;

    @Schema(description = "入侵告警状态 NORMAL/ACTIVE/RECOVERED")
    private String alarmStatus;

    @Schema(description = "人工布防开关 ARMED/DISARMED")
    private String defenseStatus;

    @Schema(description = "最近心跳时间")
    private String lastHeartbeatAt;

    @Schema(description = "最近事件时间")
    private String latestEventAt;

    @Schema(description = "展示顺序")
    private Integer sortNo;
}
