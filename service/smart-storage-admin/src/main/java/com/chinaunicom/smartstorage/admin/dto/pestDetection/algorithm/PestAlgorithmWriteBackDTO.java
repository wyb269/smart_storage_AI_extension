package com.chinaunicom.smartstorage.admin.dto.pestDetection.algorithm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 算法任务成功后的事件回写载荷。
 *
 * <p><b>只回写算法侧字段</b>：{@code algorithm_task_id / algorithm_status /
 * algorithm_result / confidence} 与本功能结论列
 * （{@code conclusion_type / conclusion_type_name / insect_species / density}），
 * <b>不得触碰 {@code process_status}</b> 与乐观锁 {@code event_version} ——
 * 算法结论独立于人工处理状态，重跑不得把已处理记录倒退回未核实。</p>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
@Data
@Schema(description = "算法结果回写检测事件载荷")
public class PestAlgorithmWriteBackDTO {

    /** 目标事件标识（ai_pest_event.id） */
    private Long eventId;

    /** 产出该结论的算法任务行标识 */
    private Long algorithmTaskId;

    private String algorithmStatus;

    /** NORMAL/ANOMALY/UNCERTAIN */
    private String algorithmResult;

    private BigDecimal confidence;

    /** 采样结论类型；算法未给出结论类型时为空，事件原结论列保持不变 */
    private String conclusionType;

    private String conclusionTypeName;

    private String insectSpecies;

    private BigDecimal density;

    /* ---------- 数据范围复核字段（仅后端构造，绝不返回前端） ---------- */

    /** 当前用户是否全域数据权限；由 Service 从登录态注入 */
    private boolean scopeUnrestricted;

    /** 授权仓库集合；由 Service 从登录态注入 */
    private List<Long> scopeWarehouseIds;
}