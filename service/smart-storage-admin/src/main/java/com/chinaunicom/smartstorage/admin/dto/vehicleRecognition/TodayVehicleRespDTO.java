package com.chinaunicom.smartstorage.admin.dto.vehicleRecognition;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 今日识别车辆列表项。
 *
 * <p>对应原型图左侧「今日识别车辆」的一张卡片：车牌号 / 车型·颜色 / 识别时间 / 识别大门，
 * 右侧一个「入库 / 出库」tag。选中一条卡片 → 右侧播放它所属识别点位的摄像头，
 * 因此本对象携带 {@code pointId}（取流入参）与 {@code eventId}（查看详情入参）。</p>
 *
 * <p>与 {@link VehicleEventRespDTO} 分开建模：左侧只关心「今天」的识别记录，字段更精简，
 * 且默认选中逻辑要按识别时间取最新一条。</p>
 *
 * <p><b>PII 提示</b>：{@code plateNo} 原样返回给前端（原型显示完整车牌），
 * 但后端日志严禁打印完整车牌，详见 Service 的掩码说明。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Data
@Schema(description = "今日识别车辆")
public class TodayVehicleRespDTO {

    @Schema(description = "识别记录标识（详情入参）")
    private String eventId;

    @Schema(description = "车牌号")
    private String plateNo;

    @Schema(description = "车辆类型")
    private String vehicleType;

    @Schema(description = "车身颜色")
    private String vehicleColor;

    @Schema(description = "行驶方向编码 IN/OUT")
    private String direction;

    @Schema(description = "行驶方向名称 入库/出库")
    private String directionName;

    @Schema(description = "识别点位标识（取流入参）")
    private String pointId;

    @Schema(description = "识别地点（点位名快照），如「东大门入口」")
    private String gateName;

    @Schema(description = "识别时间 yyyy-MM-dd HH:mm:ss")
    private String recognizedAt;

    @Schema(description = "核实状态编码 PENDING/VERIFIED/REJECTED")
    private String verifyStatus;
}
