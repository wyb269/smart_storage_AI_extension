package com.chinaunicom.smartstorage.admin.dto.personBehavior;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 人员行为记录导出请求。
 *
 * <p>字段与列表查询一致，但时间用字符串：导出走的是不分页的独立路径，
 * 由 Service 显式解析并在格式非法时报错，而不是静默丢弃时间条件。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
@Data
@Schema(description = "人员行为记录导出请求")
public class PersonExportReqDTO {

    /**
     * 发起导出的页面，取值 HISTORY / RECENT。
     *
     * <p><b>不能当作导出模板 key 使用</b>：模板 key 是
     * {@code PersonBehaviorConstant.EXPORT_BIZ_TYPE}，
     * 拿 source 去查 config 会得到 null 然后 NPE。</p>
     */
    @Schema(description = "发起导出的页面 HISTORY/RECENT")
    @Size(max = 32, message = "来源标识过长")
    private String source;

    @Schema(description = "事件编号")
    private String eventNo;

    @Schema(description = "仓库标识")
    private String warehouseId;

    @Schema(description = "监测区域标识")
    private String pointId;

    @Schema(description = "摄像头编号")
    private String cameraCode;

    @Schema(description = "行为类型编码")
    private String behaviorType;

    @Schema(description = "人工处理状态")
    private String processStatus;

    @Schema(description = "开始时间 yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @Schema(description = "结束时间 yyyy-MM-dd HH:mm:ss")
    private String endTime;

    @Schema(description = "导出条数，服务端会再按上限截断")
    private Integer limit;
}
