package com.chinaunicom.smartstorage.admin.mapper.aivision.groundWater;

import com.chinaunicom.smartstorage.admin.dto.groundWater.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.entity.groundWater.AiWaterAlgorithmTask;
import com.chinaunicom.smartstorage.admin.entity.groundWater.GroundWaterEvent;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 地面积水数据访问 Mapper，覆盖积水事件 / 监测点位 / 证据 / 审计四张表。
 *
 * <p><b>关键实现要求</b>：</p>
 * <ol>
 *   <li>每个查询都必须带仓库数据范围三态下推，非全域且无授权仓库时恒假。</li>
 *   <li>人工处理使用乐观锁，且<b>不得</b>触碰 algorithm_* 列。</li>
 *   <li>默认按发生时间倒序。</li>
 *   <li>时间列在 SQL 侧 {@code to_char} 格式化，排序用裸列。</li>
 *   <li>单参数方法也必须加 {@code @Param}，否则运行期抛
 *       「Parameter 'xxx' not found」。</li>
 * </ol>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
public interface GroundWaterMapper extends MPJBaseMapper<GroundWaterEvent> {

    /**
     * 查询当前用户可见的监测点位状态。
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     */
    List<MonitorPointStatusRespDTO> listMonitorPoint(@Param("unrestricted") boolean unrestricted,
                                                    @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询最近积水事件，按发生时间倒序。
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     * @param limit        展示条数
     */
    List<WaterEventRespDTO> listRecentEvent(@Param("unrestricted") boolean unrestricted,
                                            @Param("warehouseIds") List<Long> warehouseIds,
                                            @Param("limit") int limit);

    /**
     * 按条件查询事件（分页由 {@code @QueryByPage} 切面完成）。
     *
     * <p>{@code @Param("query")} 不可省略：XML 内以 {@code query.xxx} 引用属性，
     * 单参数不加注解时 MyBatis 会把实体作为 OGNL 根对象，
     * 导致 {@code query.eventNo} 解析不到而整个 where 退化。</p>
     */
    List<WaterEventRespDTO> listEventByCondition(@Param("query") WaterEventQueryReqDTO query);

    /**
     * 按条件查询事件（导出用，不分页，受条数上限约束）。
     */
    List<WaterEventRespDTO> listEventForExport(@Param("query") WaterEventQueryReqDTO query,
                                               @Param("limit") int limit);

    /**
     * 带数据范围查询事件主记录，用于详情前置校验与取 event_no。
     *
     * @return 越权或不存在时返回 null，不区分两种情况以避免泄露对象存在性
     */
    WaterEventRespDTO getEventSummaryInScope(@Param("eventId") Long eventId,
                                             @Param("unrestricted") boolean unrestricted,
                                             @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询事件详情（带数据范围校验）。
     */
    WaterEventDetailRespDTO getEventDetailInScope(@Param("eventId") Long eventId,
                                                  @Param("unrestricted") boolean unrestricted,
                                                  @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询某事件的全部证据。短时地址由 Service 经 MediaGateway 签发。
     */
    List<WaterEvidenceRespDTO> listEvidenceByEventId(@Param("eventId") Long eventId);

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

    /* ==================== 算法任务表 ai_water_algorithm_task ==================== */

    /**
     * 落一行算法任务（提交重跑时的 QUEUED 行）。
     *
     * <p>id 由仓储层发号（雪花），无序列无默认值，与其余 ai_water_* 表一致。</p>
     */
    int insertAlgorithmTask(@Param("task") AiWaterAlgorithmTask task);

    /**
     * 按 task_no 查询任务，带数据范围（三态下推同其余查询，共 6/8 处）。
     */
    AiWaterAlgorithmTask getAlgorithmTaskByTaskNo(@Param("taskNo") String taskNo,
                                                  @Param("unrestricted") boolean unrestricted,
                                                  @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 更新任务的执行状态与产出（按 task_no 定位，WHERE 同样带数据范围，7/8 处）。
     *
     * <p>只写算法侧列；任务行永不携带人工结论，不存在「处理覆盖算法」的通道。</p>
     */
    int updateAlgorithmTaskOutcome(@Param("task") AiWaterAlgorithmTask task,
                                   @Param("updatedBy") String updatedBy,
                                   @Param("updatedAt") LocalDateTime updatedAt,
                                   @Param("unrestricted") boolean unrestricted,
                                   @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询某事件的算法任务，按 started_at 倒序（8/8 处范围下推）。
     */
    List<AiWaterAlgorithmTask> listAlgorithmTasksByEventId(@Param("eventId") Long eventId,
                                                           @Param("unrestricted") boolean unrestricted,
                                                           @Param("warehouseIds") List<Long> warehouseIds,
                                                           @Param("limit") int limit);

    /**
     * 事件算法侧字段回填。
     *
     * <p>⚠️ 只写 algorithm_* / confidence / water_type，
     * 不触碰 process_status 与 event_version（算法不产生人工结论、不搅动乐观锁）。
     * confidence / water_type 传 null 时 COALESCE 保留事件原值——
     * water_type 是 NOT NULL，失败/超时任务没有新结论，不能把它抹成空。</p>
     *
     * <p>范围保护：只在仓储层 saveAlgorithmTaskOutcome 的事务内被调用，
     * 调用前任务行已经过带范围查询与带范围更新，事件与任务同仓同点位，
     * 因此本 UPDATE 按主键单行写入即可，不再重复叠加仓库条件。</p>
     */
    int writeBackEventAlgorithm(@Param("eventId") Long eventId,
                                @Param("taskId") Long taskId,
                                @Param("algorithmStatus") String algorithmStatus,
                                @Param("algorithmResult") String algorithmResult,
                                @Param("confidence") BigDecimal confidence,
                                @Param("waterType") String waterType,
                                @Param("updatedBy") String updatedBy,
                                @Param("updatedAt") LocalDateTime updatedAt);
}
