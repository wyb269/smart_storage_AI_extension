package com.chinaunicom.smartstorage.admin.dto.doorAnomaly;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 事件导出请求。
 *
 * <p>导出遵守第 18.5 节「导出规则」与第 29.5 节「导出安全」：</p>
 * <ul>
 *   <li>导出字段严格限定，不嵌入图片证据。</li>
 *   <li>限制单次导出数量与频率。</li>
 *   <li>Excel 公式注入防护由服务端负责，前端不承担该职责。</li>
 *   <li>不把短时媒体地址写入导出文件。</li>
 * </ul>
 *
 * @author 仓门异动功能
 */
@Data
@Schema(description = "事件导出请求")
public class DoorAnomalyExportReqDTO {

    /**
     * 导出范围：首页最近事件或历史查询条件。
     */
    @Pattern(regexp = "RECENT|HISTORY", message = "导出范围只能是RECENT或HISTORY")
    @Schema(description = "导出范围 RECENT/HISTORY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String source;

    @Schema(description = "事件编号")
    private String eventNo;

    @Schema(description = "仓库标识")
    private String warehouseId;

    @Schema(description = "仓门标识")
    private String doorId;

    @Schema(description = "摄像头编号")
    private String cameraCode;

    @Schema(description = "异动类型编码")
    private String anomalyType;

    @Schema(description = "人工处理状态")
    private String processStatus;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;

    /**
     * 单次导出上限。
     * <p>第 18.5 节要求超过约 5000 条改用异步导出任务；
     * 本期采用同步导出，因此上限设为 5000 并在服务端二次校验。</p>
     */
    @Min(value = 1, message = "导出条数必须大于0")
    @Max(value = 5000, message = "单次导出不能超过5000条")
    @Schema(description = "单次导出条数上限，最大5000")
    private Integer limit = 5000;
}
