package com.chinaunicom.smartstorage.admin.mapper.aivision.doorAnomaly;

import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.AnomalyEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.DoorStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.doorAnomaly.AiAnomalyEvent;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 异动事件 Mapper。
 *
 * <p>依据 AI_HANDOFF.md 第 23 节，负责事件主表的数据访问。</p>
 *
 * <p><b>关键实现要求</b>：</p>
 * <ol>
 *   <li>人工处理必须使用乐观锁 {@code UPDATE ... WHERE id = ? AND event_version = ?}，
 *       版本不匹配时更新 0 行，由 Service 判定并发冲突（第 20.6 节）。</li>
 *   <li>该更新<b>不得触碰</b> {@code algorithm_status} 与 {@code algorithm_result}，
 *       人工处理不得覆盖或删除算法结果（第 23 节）。</li>
 *   <li>默认按 {@code occurred_at DESC} 排序（第 18.3 节）。</li>
 * </ol>
 *
 * @author 仓门异动功能
 */
public interface AiAnomalyEventMapper extends MPJBaseMapper<AiAnomalyEvent> {

    /**
     * 查询仓门状态列表（含摄像头编号），按展示顺序排列。
     *
     * <p>P0-6：数据范围以<b>仓库</b>为强制维度下推。
     * {@code unrestricted=false} 且 {@code warehouseIds} 为空时
     * 下推恒假条件，返回空列表——绝不因取不到授权维度而放宽为全表。</p>
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     */
    List<DoorStatusRespDTO> listDoorStatus(@Param("unrestricted") boolean unrestricted,
                                           @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询最近事件，按发生时间倒序。
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     * @param limit        展示条数
     */
    List<AnomalyEventRespDTO> listRecentEvent(@Param("unrestricted") boolean unrestricted,
                                              @Param("warehouseIds") List<Long> warehouseIds,
                                              @Param("limit") int limit);

    /**
     * 按条件查询事件（分页由 {@code @QueryByPage} 切面完成）。
     *
     * <p>{@code @Param("query")} 不可省略：XML 内以 {@code query.xxx} 引用属性，
     * 单参数不加注解时 MyBatis 会把实体直接作为 OGNL 根对象，
     * 导致 {@code query.eventNo} 解析不到而整个 where 退化。</p>
     *
     * @param query 查询条件
     */
    List<AnomalyEventRespDTO> listEventByCondition(@Param("query") EventQueryReqDTO query);

    /**
     * 按条件查询事件（导出用，不分页，受条数上限约束）。
     *
     * @param query 查询条件
     * @param limit 条数上限
     */
    List<AnomalyEventRespDTO> listEventForExport(@Param("query") EventQueryReqDTO query,
                                                 @Param("limit") int limit);

    /**
     * 带数据范围查询事件主记录（P0-6 越权校验）。
     *
     * <p>仓库约束<b>在 SQL 里生效</b>而不是取回整行后在 Java 判断：
     * 详情列表未 SELECT {@code org_id}/{@code region_id}，
     * Java 侧无法完成等值校验，越权防护会静默空转。</p>
     *
     * @param eventId      事件标识
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     * @return 越权或不存在时返回 null，不区分两种情况以避免泄露对象存在性
     */
    AnomalyEventRespDTO getEventSummaryInScope(@Param("eventId") Long eventId,
                                               @Param("unrestricted") boolean unrestricted,
                                               @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询事件详情（含算法任务字段），带数据范围校验。
     *
     * <p>与 {@link #getEventSummaryInScope} 的区别只在返回列：本方法
     * LEFT JOIN {@code ai_algorithm_task} 补齐模型、版本、推理起止与耗时、
     * 模拟数据标识等「仅详情」字段（第 17.2、17.3、39.6 节）。</p>
     *
     * @param eventId      事件标识
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     * @return 越权或不存在时返回 null
     */
    EventDetailRespDTO getEventDetailInScope(@Param("eventId") Long eventId,
                                             @Param("unrestricted") boolean unrestricted,
                                             @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 以乐观锁方式提交人工处理结果。
     *
     * <p>SQL 形如：
     * {@code UPDATE ai_anomaly_event SET process_status=?, processed_by=?,
     * processed_at=?, process_opinion=?, event_version=event_version+1, updated_at=?
     * WHERE id=? AND event_version=? AND process_status NOT IN (...终态...)}</p>
     *
     * @param eventId      事件标识
     * @param eventVersion 用户读取到的事件版本号
     * @param processStatus 处理后状态
     * @param processedBy  处理人（后端从登录态获取）
     * @param processOpinion 处理意见
     * @param updatedAt    更新时间
     * @return 受影响行数；1 成功，0 表示版本冲突或状态不允许
     */
    int updateProcessWithVersion(@Param("eventId") Long eventId,
                                 @Param("eventVersion") Integer eventVersion,
                                 @Param("processStatus") String processStatus,
                                 @Param("processedBy") String processedBy,
                                 @Param("processOpinion") String processOpinion,
                                 @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 「重新识别」成功后回写事件的算法维度（契约 §1）。
     *
     * <p>只写算法相关列，<b>绝不触碰 process_status</b>（人工处理状态独立，第 23 节），
     * 也不改 event_version（重跑不是人工核实，不应扰动乐观锁版本）。</p>
     *
     * <p>anomalyType 为空时保留事件原值（NOT NULL，不能被算法空结论抹掉）；
     * algorithmResult 仅在算法成功产出结论时写入，失败/超时不得伪造为 NORMAL。</p>
     *
     * <p>带数据范围（P0-6）：非全域且仓库不在授权集合时更新 0 行。</p>
     *
     * @return 受影响行数；1 成功，0 表示越权或事件不存在
     */
    int updateAlgorithmFields(@Param("eventId") Long eventId,
                              @Param("algorithmTaskId") Long algorithmTaskId,
                              @Param("algorithmStatus") String algorithmStatus,
                              @Param("algorithmResult") String algorithmResult,
                              @Param("confidence") BigDecimal confidence,
                              @Param("anomalyType") String anomalyType,
                              @Param("updatedBy") String updatedBy,
                              @Param("updatedAt") LocalDateTime updatedAt,
                              @Param("unrestricted") boolean unrestricted,
                              @Param("warehouseIds") List<Long> warehouseIds);
}