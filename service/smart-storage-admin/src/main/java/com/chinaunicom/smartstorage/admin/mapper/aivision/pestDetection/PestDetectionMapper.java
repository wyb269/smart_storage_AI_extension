package com.chinaunicom.smartstorage.admin.mapper.aivision.pestDetection;

import com.chinaunicom.smartstorage.admin.dto.pestDetection.MonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.algorithm.PestAlgorithmWriteBackDTO;
import com.chinaunicom.smartstorage.admin.entity.pestDetection.AiPestAlgorithmTask;
import com.chinaunicom.smartstorage.admin.entity.pestDetection.PestDetectionEvent;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 粮面虫害数据访问 Mapper，覆盖检测事件 / 粮面点位 / 证据 / 审计四张表。
 *
 * <p><b>关键实现要求</b>：</p>
 * <ol>
 *   <li>每个查询都必须带仓库数据范围三态下推，非全域且无授权仓库时恒假。</li>
 *   <li>人工处理使用乐观锁，且<b>不得</b>触碰 algorithm_* 列。</li>
 *   <li>默认按采样时间倒序。</li>
 *   <li>时间列在 SQL 侧 {@code to_char} 格式化，排序用裸列。</li>
 * </ol>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
public interface PestDetectionMapper extends MPJBaseMapper<PestDetectionEvent> {

    /**
     * 查询当前用户可见的粮面点位状态。
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     */
    List<MonitorPointRespDTO> listMonitorPoint(@Param("unrestricted") boolean unrestricted,
                                               @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询最近检测记录，按采样时间倒序。
     */
    List<PestEventRespDTO> listRecentEvent(@Param("unrestricted") boolean unrestricted,
                                           @Param("warehouseIds") List<Long> warehouseIds,
                                           @Param("limit") int limit);

    /**
     * 按条件查询记录（分页由 {@code @QueryByPage} 切面完成）。
     *
     * <p>{@code @Param("query")} 不可省略：XML 内以 {@code query.xxx} 引用属性，
     * 单参数不加注解时 MyBatis 会把实体作为 OGNL 根对象，
     * 导致 {@code query.eventNo} 解析不到而整个 where 退化。</p>
     */
    List<PestEventRespDTO> listEventByCondition(@Param("query") PestEventQueryReqDTO query);

    /**
     * 按条件查询记录（导出用，不分页，受条数上限约束）。
     */
    List<PestEventRespDTO> listEventForExport(@Param("query") PestEventQueryReqDTO query,
                                              @Param("limit") int limit);

    /**
     * 带数据范围查询记录主记录，用于详情前置校验与取 event_no。
     *
     * @return 越权或不存在时返回 null，不区分两种情况以避免泄露对象存在性
     */
    PestEventRespDTO getEventSummaryInScope(@Param("eventId") Long eventId,
                                            @Param("unrestricted") boolean unrestricted,
                                            @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询记录详情（带数据范围校验）。
     */
    PestEventDetailRespDTO getEventDetailInScope(@Param("eventId") Long eventId,
                                                 @Param("unrestricted") boolean unrestricted,
                                                 @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询某记录的全部证据。短时地址由 Service 经 MediaGateway 签发。
     */
    List<PestEvidenceRespDTO> listEvidenceByEventId(@Param("eventId") Long eventId);

    /**
     * 查询某记录的全部处理审计，按时间正序。
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
     * 追加一条处理审计记录（只插入，永不 UPDATE / DELETE）。
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

    /* ==================== 算法任务表 ai_pest_algorithm_task ==================== */

    /** 落一行算法任务（QUEUED/RUNNING，is_simulated=TRUE） */
    int insertAlgorithmTask(@Param("task") AiPestAlgorithmTask task);

    /** 按任务行 id 更新执行结果（状态/结论/置信度/模型版本/耗时/检测框/错误码） */
    int updateAlgorithmTaskResult(@Param("task") AiPestAlgorithmTask task);

    /**
     * 按任务编号查询算法任务（带数据范围）。
     *
     * @return 越权或不存在时返回 null，不区分两种情况以避免泄露对象存在性
     */
    PestAlgorithmTaskRespDTO getAlgorithmTaskByTaskNoInScope(@Param("taskNo") String taskNo,
                                                             @Param("unrestricted") boolean unrestricted,
                                                             @Param("warehouseIds") List<Long> warehouseIds);

    /** 按事件查询历史算法任务，started_at 倒序，供详情弹窗任务表用 */
    List<PestAlgorithmTaskRespDTO> listAlgorithmTasksByEventId(@Param("eventId") Long eventId,
                                                               @Param("unrestricted") boolean unrestricted,
                                                               @Param("warehouseIds") List<Long> warehouseIds,
                                                               @Param("limit") int limit);

    /**
     * 算法任务成功后回写事件的算法侧结论列。
     *
     * <p><b>只动 algorithm_* 与本功能结论列，绝不触碰 process_status / event_version</b>；
     * 结论列（conclusion_* / insect_species / density）在载荷里为空时保持事件原值。</p>
     */
    int writeBackEventAlgorithmResult(@Param("writeBack") PestAlgorithmWriteBackDTO writeBack,
                                      @Param("updatedAt") LocalDateTime updatedAt);
}
