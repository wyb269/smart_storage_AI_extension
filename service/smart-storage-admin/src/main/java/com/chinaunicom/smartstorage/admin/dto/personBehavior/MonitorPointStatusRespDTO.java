package com.chinaunicom.smartstorage.admin.dto.personBehavior;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 监测区域状态响应。
 *
 * <p>对应原型图左侧「人员监测区域分布」网格的一张卡片。
 * 页面展示的「正常 / 人员聚集 / 未戴安全帽 / 离线 / 未知」由前端综合
 * {@code deviceStatus}、{@code alarmStatus} 与心跳新鲜度算出，
 * 后端不返回合并后的单一字符串。</p>
 *
 * <p>{@code alarmBehaviorType} 只在综合展示态为「异常」时被前端取用，
 * 用来决定异常卡片上写「人员聚集」还是「未戴安全帽」——
 * 共享基座的综合态只有一个 ABNORMAL，区分不了具体行为。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
@Data
@Schema(description = "人员行为监测区域状态")
public class MonitorPointStatusRespDTO {

    @Schema(description = "监测区域标识")
    private String pointId;

    @Schema(description = "区域编码，如 AREA-001")
    private String pointCode;

    @Schema(description = "区域名称，卡片标题")
    private String pointName;

    @Schema(description = "所属仓库标识")
    private String warehouseId;

    @Schema(description = "所属仓库名称")
    private String warehouseName;

    @Schema(description = "绑定的设备侧摄像头标识")
    private String cameraId;

    @Schema(description = "绑定的摄像头业务编码，如 CAM-001")
    private String cameraCode;

    @Schema(description = "视频平台侧标识，非敏感")
    private String platformCameraId;

    @Schema(description = "设备连接状态 ONLINE/OFFLINE/UNKNOWN")
    private String deviceStatus;

    @Schema(description = "行为告警状态 NORMAL/ACTIVE/RECOVERED")
    private String alarmStatus;

    @Schema(description = "当前告警对应的行为类型 CROWD/NO_HELMET/FALL/INTRUSION/OTHER")
    private String alarmBehaviorType;

    @Schema(description = "最近心跳时间")
    private String lastHeartbeatAt;

    @Schema(description = "最近事件时间")
    private String latestEventAt;

    @Schema(description = "展示顺序")
    private Integer sortNo;
}
