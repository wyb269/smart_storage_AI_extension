package com.chinaunicom.smartstorage.admin.mapper.aivision.perimeterIntrusion;

import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.entity.perimeterIntrusion.AiPerimeterAlgorithmTask;
import com.chinaunicom.smartstorage.admin.entity.perimeterIntrusion.PerimeterIntrusionEvent;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 周界入侵数据访问 Mapper，覆盖入侵事件 / 防区段配置 / 证据 / 审计四张表。
 *
 * <p><b>关键实现要求</b>：</p>
 * <ol>
 *   <li>每个查询都必须带仓库数据范围三态下推，非全域且无授权仓库时恒假。</li>
 *   <li>人工处理使用乐观锁，且<b>不得</b>触碰 algorithm_* 列。</li>
 *   <li>默认按发生时间倒序。</li>
 *   <li>时间列在 SQL 侧 {@code to_char} 格式化，排序用裸列。</li>
 * </ol>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
public interface PerimeterIntrusionMapper extends MPJBaseMapper<PerimeterIntrusionEvent> {

    /**
     * 查询当前用户可见的防区段状态。
     *
     * <p>防区下拉的筛选在前端本地完成，因此本方法<b>不接受</b> zone 参数：
     * 一次取全量再本地过滤，才能保证「切换筛选不打断取流、默认选中稳定」。</p>
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     */
    List<MonitorPointStatusRespDTO> listMonitorPoint(@Param("unrestricted") boolean unrestricted,
                                                     @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询最近入侵事件，按发生时间倒序。
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     * @param limit        展示条数
     */
    List<PerimeterEventRespDTO> listRecentEvent(@Param("unrestricted") boolean unrestricted,
                                                @Param("warehouseIds") List<Long> warehouseIds,
                                                @Param("limit") int limit);

    /**
     * 按条件查询事件（分页由 {@code @QueryByPage} 切面完成）。
     *
     * <p>{@code @Param("query")} 不可省略：XML 内以 {@code query.xxx} 引用属性，
     * 单参数不加注解时 MyBatis 会把实体作为 OGNL 根对象，
     * 导致 {@code query.eventNo} 解析不到而整个 where 退化。</p>
     */
    List<PerimeterEventRespDTO> listEventByCondition(@Param("query") PerimeterEventQueryReqDTO query);

    /**
     * 按条件查询事件（导出用，不分页，受条数上限约束）。
     */
    List<PerimeterEventRespDTO> listEventForExport(@Param("query") PerimeterEventQueryReqDTO query,
                                                   @Param("limit") int limit);

    /**
     * 带数据范围查询事件主记录，用于详情前置校验与取 event_no。
     *
     * @return 越权或不存在时返回 null，不区分两种情况以避免泄露对象存在性
     */
    PerimeterEventRespDTO getEventSummaryInScope(@Param("eventId") Long eventId,
                                                 @Param("unrestricted") boolean unrestricted,
                                                 @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询事件详情（带数据范围校验）。
     */
    PerimeterEventDetailRespDTO getEventDetailInScope(@Param("eventId") Long eventId,
                                                      @Param("unrestricted") boolean unrestricted,
                                                      @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询某事件的全部证据。短时地址由 Service 经 MediaGateway 签发。
     */
    List<PerimeterEvidenceRespDTO> listEvidenceByEventId(@Param("eventId") Long eventId);

    /**
     * 查询某事件的全部处理记录，按时间正序。
     */
    List<ProcessRecordRespDTO> listProcessRecords(@Param("eventId") Long eventId);

    /**
     * 以乐观锁方式提交人工处理结果。
     *
     * @return 受影响行数；1 成功，0 表示版本冲突或状态不允许
     */
    int updateProcessWithVersion(@Param("eventId") Long eventId,
                                 @Param("eventVersion") Integer eventVersion,
                                 @Param("processStatus") String processStatus,
                                 @Param("processedBy") String processedBy,
                                 @Param("processOpinion") String processOpinion,
                                 @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 追加一条处理审计记录。
     *
     * <p>只插入，永不 UPDATE / DELETE。</p>
     */
    int appendProcessRecord(@Param("id") Long id,
                            @Param("eventId") Long eventId,
                            @Param("eventNo") String eventNo,
                            @Param("statusBefore") String statusBefore,
                            @Param("statusAfter") String statusAfter,
                            @Param("processResult") String processResult,
                            @Param("processOpinion") String processOpinion,
                            @Param("processedBy") String processedBy,
                            @Param("processedAt") LocalDateTime processedAt,
                            @Param("source") String source,
                            @Param("createdAt") LocalDateTime createdAt);

    /* ==================== 算法任务（ai_perimeter_algorithm_task，契约 §1） ==================== */

    /**
     * 落一行算法任务。id 由仓储实现发号后传入（本表无序列，与其余 ai_perimeter_* 一致）。
     */
    int insertAlgorithmTask(AiPerimeterAlgorithmTask task);

    /**
     * 按任务编号查任务（带数据范围，越权与不存在都返回 null 不区分）。
     */
    AiPerimeterAlgorithmTask selectAlgorithmTaskByNo(@Param("taskNo") String taskNo,
                                                     @Param("unrestricted") boolean unrestricted,
                                                     @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询某事件的算法任务，按 started_at 倒序（NULLS LAST），最多 limit 条。
     */
    List<AiPerimeterAlgorithmTask> listAlgorithmTasksByEvent(@Param("eventId") Long eventId,
                                                             @Param("unrestricted") boolean unrestricted,
                                                             @Param("warehouseIds") List<Long> warehouseIds,
                                                             @Param("limit") int limit);

    /**
     * 轮询后更新任务执行结果，按 taskNo 定位。
     *
     * <p>调用前 Service 已通过 {@link #selectAlgorithmTaskByNo} 做过范围校验，
     * 与 processEvent 的「先范围读、后按主键写」模式一致，
     * 因此本语句不再重复拼范围条件。</p>
     */
    int updateAlgorithmTaskResult(AiPerimeterAlgorithmTask task);

    /**
     * 算法终态回写事件的算法列。
     *
     * <p><b>不触碰 process_status / event_version</b>：算法结论独立于人工核实，
     * 重跑不得顶掉人工状态，也不制造乐观锁冲突。</p>
     */
    int updateEventAlgorithmResult(@Param("eventId") Long eventId,
                                   @Param("taskId") Long taskId,
                                   @Param("algorithmStatus") String algorithmStatus,
                                   @Param("algorithmResult") String algorithmResult,
                                   @Param("confidence") BigDecimal confidence,
                                   @Param("intrusionType") String intrusionType,
                                   @Param("updatedBy") String updatedBy,
                                   @Param("updatedAt") LocalDateTime updatedAt);
}
