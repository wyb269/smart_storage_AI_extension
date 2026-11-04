package com.chinaunicom.smartstorage.admin.dto.doorAnomaly;

import com.chinaunicom.smartstorage.common.bean.TimeBaseQueryDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 历史事件分页查询条件。
 *
 * <p>沿用项目既有继承体系：{@code TimeBaseQueryDTO} 提供
 * startTime / endTime，再向上继承 pageNo / pageSize 与
 * regionId / orgId，满足行政区数据范围过滤。</p>
 *
 * <p>查询字段对齐 AI_HANDOFF.md 第 21.2 节。</p>
 *
 * @author 仓门异动功能
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "历史事件查询条件")
public class EventQueryReqDTO extends TimeBaseQueryDTO {

    @Schema(description = "事件编号")
    private String eventNo;

    /**
     * 仓库筛选。
     *
     * <p><b>必须为数值类型</b>：{@code ai_anomaly_event.warehouse_id} 是 BIGINT，
     * 而 JDBC URL 未设置 {@code stringtype=unspecified}，
     * 用 String 绑定会以 varchar 发送，PostgreSQL 报
     * {@code operator does not exist: bigint = character varying}。
     * 前端传数字字符串（"9001"）由 Jackson 正常转换，空字符串转为 null 即不参与筛选。</p>
     */
    @Schema(description = "仓库标识")
    private Long warehouseId;

    /** 仓门筛选。类型原因同 {@link #warehouseId}（{@code door_id} 同为 BIGINT） */
    @Schema(description = "仓门标识")
    private Long doorId;

    @Schema(description = "摄像头编号")
    private String cameraCode;

    @Schema(description = "异动类型编码")
    private String anomalyType;

    @Schema(description = "人工处理状态")
    private String processStatus;

    /**
     * 排序方向，默认按发生时间倒序（第 18.3 节）。
     */
    @Schema(description = "是否按发生时间倒序，默认 true")
    private Boolean orderByOccurredDesc = true;

    /* ==================== 数据范围条件（后端注入，不可由前端传入） ==================== */

    /**
     * 数据范围：是否具备全域权限。
     *
     * <p><b>安全关键</b>：只能由 Service 从登录态构造，前端传入一律忽略。
     * 为 false 且 {@code scopeWarehouseIds} 为空时，Mapper 下推恒假条件，
     * 即「未解析出授权仓库」不等于「不限制」。</p>
     */
    @Schema(description = "数据范围：是否全域（后端注入）", hidden = true)
    @JsonIgnore
    private boolean scopeUnrestricted;

    /**
     * 数据范围：授权仓库标识集合。
     *
     * <p>使用 {@code Long} 与 {@code ai_anomaly_event.warehouse_id BIGINT}
     * 对齐；以字符串绑定会触发 PostgreSQL
     * {@code bigint = character varying} 无匹配运算符。</p>
     */
    @Schema(description = "数据范围：授权仓库集合（后端注入）", hidden = true)
    @JsonIgnore
    private List<Long> scopeWarehouseIds;
}
