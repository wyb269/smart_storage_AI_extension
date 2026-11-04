package com.chinaunicom.smartstorage.admin.dto.vehicleRecognition;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 出入事件记录导出请求。
 *
 * <p>字段与列表查询一致，但时间用字符串：导出走的是不分页的独立路径，
 * 由 Service 显式解析并在格式非法时报错，而不是静默丢弃时间条件。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Data
@Schema(description = "车辆识别记录导出请求")
public class VehicleExportReqDTO {

    /**
     * 发起导出的页面，取值 HISTORY / RECENT。
     *
     * <p><b>不能当作导出模板 key 使用</b>：模板 key 是
     * {@code VehicleRecognitionConstant.EXPORT_BIZ_TYPE}，
     * 拿 source 去查 config 会得到 null 然后 NPE。</p>
     */
    @Schema(description = "发起导出的页面 HISTORY/RECENT")
    @Size(max = 32, message = "来源标识过长")
    private String source;

    @Schema(description = "车牌号")
    private String plateNo;

    @Schema(description = "车辆类型")
    private String vehicleType;

    @Schema(description = "行驶方向 IN/OUT")
    private String direction;

    @Schema(description = "识别地点")
    private String gateName;

    @Schema(description = "核实状态")
    private String verifyStatus;

    @Schema(description = "仓库标识")
    private String warehouseId;

    @Schema(description = "识别点位标识")
    private String pointId;

    @Schema(description = "开始时间 yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @Schema(description = "结束时间 yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @Schema(description = "导出条数，服务端会再按上限截断")
    private Integer limit;
}
