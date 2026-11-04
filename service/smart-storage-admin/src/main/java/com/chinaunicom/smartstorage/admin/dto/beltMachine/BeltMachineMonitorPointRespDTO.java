package com.chinaunicom.smartstorage.admin.dto.beltMachine;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 皮带机监测点位状态响应。
 *
 * <p>对应原型图左侧「皮带机情况概况」网格的一张卡片，卡片标题是设备名。
 * 页面展示的「正常 / 报警 / 离线 / 未部署」由前端综合
 * {@code deviceStatus}、{@code alarmStatus}、心跳新鲜度与
 * {@code cameraId} 是否为空算出，后端不返回合并后的单一字符串。</p>
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
@Data
@Schema(description = "皮带机监测点位状态")
public class BeltMachineMonitorPointRespDTO {

    @Schema(description = "监测点位标识，取流接口入参")
    private String pointId;

    @Schema(description = "皮带机业务编码，如 BELT-001")
    private String deviceCode;

    @Schema(description = "皮带机名称，如 一号皮带机（卡片标题）")
    private String deviceName;

    @Schema(description = "安装作业区名称")
    private String areaName;

    @Schema(description = "所属仓库标识")
    private String warehouseId;

    @Schema(description = "所属仓库名称")
    private String warehouseName;

    /**
     * 绑定的摄像头平台标识。
     *
     * <p>为空即「该点位没有绑定摄像头」，与 {@code deviceStatus=UNKNOWN}
     * 一起被前端判为「未部署」。前端据此决定是否展示播放区。</p>
     */
    @Schema(description = "绑定摄像头平台标识，为空表示未绑定摄像头")
    private String cameraId;

    @Schema(description = "绑定摄像头业务编码，如 CAM-003")
    private String cameraCode;

    @Schema(description = "绑定摄像头位置名称")
    private String cameraName;

    @Schema(description = "设备连接状态 ONLINE/OFFLINE/UNKNOWN")
    private String deviceStatus;

    @Schema(description = "行为告警状态 NORMAL/ACTIVE/RECOVERED")
    private String alarmStatus;

    @Schema(description = "最近心跳时间")
    private String lastHeartbeatAt;

    @Schema(description = "最近事件时间")
    private String latestEventAt;

    @Schema(description = "展示顺序")
    private Integer sortNo;
}
