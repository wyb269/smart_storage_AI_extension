package com.chinaunicom.smartstorage.admin.mapper.aivision.beltMachine;

import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineMonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.entity.beltMachine.BeltMachineAlgorithmTask;
import com.chinaunicom.smartstorage.admin.entity.beltMachine.BeltMachineEvent;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 皮带机下站人数据访问 Mapper，覆盖事件 / 监测点位 / 证据 / 审计四张表。
 *
 * <p><b>关键实现要求</b>：</p>
 * <ol>
 *   <li>每个查询都必须带仓库数据范围三态下推，非全域且无授权仓库时恒假。</li>
 *   <li>人工处理使用乐观锁，且<b>不得</b>触碰 algorithm_* 列。</li>
 *   <li>默认按发生时间倒序。</li>
 *   <li>时间列在 SQL 侧 {@code to_char} 格式化，排序用裸列。</li>
 *   <li>单参数方法也必须加 {@code @Param}，否则运行期报
 *       「Parameter 'xxx' not found」。</li>
 * </ol>
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
public interface BeltMachineMapper extends MPJBaseMapper<BeltMachineEvent> {

    /**
     * 查询当前用户可见的皮带机监测点位状态。
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     */
    List<BeltMachineMonitorPointRespDTO> listMonitorPoint(@Param("unrestricted") boolean unrestricted,
                                                          @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询最近皮带机事件，按发生时间倒序。
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     * @param limit        展示条数
     */
    List<BeltMachineEventRespDTO> listRecentEvent(@Param("unrestricted") boolean unrestricted,
                                                  @Param("warehouseIds") List<Long> warehouseIds,
                                                  @Param("limit") int limit);

    /**
     * 按条件查询事件（分页由 {@code @QueryByPage} 切面完成）。
     *
     * <p>{@code @Param("query")} 不可省略：XML 内以 {@code query.xxx} 引用属性，
     * 单参数不加注解时 MyBatis 会把实体作为 OGNL 根对象，
     * 导致 {@code query.eventNo} 解析不到而整个 where 退化。</p>
     */
    List<BeltMachineEventRespDTO> listEventByCondition(@Param("query") BeltMachineEventQueryReqDTO query);

    /**
     * 按条件查询事件（导出用，不分页，受条数上限约束）。
     */
    List<BeltMachineEventRespDTO> listEventForExport(@Param("query") BeltMachineEventQueryReqDTO query,
                                                     @Param("limit") int limit);

    /**
     * 带数据范围查询事件主记录，用于详情前置校验与取 event_no。
     *
     * @return 越权或不存在时返回 null，不区分两种情况以避免泄露对象存在性
     */
    BeltMachineEventRespDTO getEventSummaryInScope(@Param("eventId") Long eventId,
                                                   @Param("unrestricted") boolean unrestricted,
                                                   @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询事件详情（带数据范围校验）。
     */
    BeltMachineEventDetailRespDTO getEventDetailInScope(@Param("eventId") Long eventId,
                                                        @Param("unrestricted") boolean unrestricted,
                                                        @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询某事件的全部证据。短时地址由 Service 经 MediaGateway 签发。
     */
    List<BeltMachineEvidenceRespDTO> listEvidenceByEventId(@Param("eventId") Long eventId);

    /**
     * 查询某事件的全部处理记录，按时间正序。
     */
    List<BeltMachineProcessRecordRespDTO> listProcessRecords(@Param("eventId") Long eventId);

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

    /* ==================== 算法任务（重新识别链路） ==================== */

    /**
     * 插入一行算法任务。id 由仓储层发号（雪花），task_no 唯一。
     *
     * <p>「重新识别」受理后必须先落一行 QUEUED/RUNNING，
     * 轮询接口凭 task_no 找回它并同步网关最新进度。</p>
     */
    int insertAlgorithmTask(@Param("task") BeltMachineAlgorithmTask task);

    /**
     * 按任务编号 + 数据范围查询算法任务。
     *
     * <p>轮询接口每一拍都会调它；越权与不存在同样返回 null，
     * 不泄露任务是否存在。</p>
     */
    BeltMachineAlgorithmTask getAlgorithmTaskByNoInScope(@Param("taskNo") String taskNo,
                                                         @Param("unrestricted") boolean unrestricted,
                                                         @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 以 id 更新任务的执行结果列（状态/结论/置信度/模型/版本/耗时/检测框/错误码等）。
     *
     * <p>调用方是「先查库拿到带范围内的行、再取网关最新结果」的轮询链路，
     * 因此这里不再重复范围条件；新增其它调用点时必须自行校验。</p>
     */
    int updateAlgorithmTaskResult(@Param("task") BeltMachineAlgorithmTask task);

    /**
     * 查询某事件的全部算法任务，按开始时间倒序（开始时间为空时退化为创建时间）。
     *
     * <p>必须在事件已按数据范围校验之后调用，与 listEvidenceByEventId 同一约定。</p>
     */
    List<BeltMachineAlgorithmTask> listAlgorithmTasksByEventId(@Param("eventId") Long eventId,
                                                              @Param("limit") int limit);

    /**
     * 算法任务成功后回写事件的算法列。
     *
     * <p><b>只写 algorithm_task_id / algorithm_status / algorithm_result /
     * confidence / conclusion_type，绝不触碰 process_status 等人工处理列</b>
     * （AI_HANDOFF：算法结论不得被机器重跑改写人工状态，反之亦然）。
     * conclusion_type 为空时保留事件原值，避免把 NOT NULL 列写崩。</p>
     */
    int updateEventAlgorithmWriteback(@Param("eventId") Long eventId,
                                      @Param("taskId") Long taskId,
                                      @Param("algorithmStatus") String algorithmStatus,
                                      @Param("algorithmResult") String algorithmResult,
                                      @Param("conclusionType") String conclusionType,
                                      @Param("confidence") BigDecimal confidence,
                                      @Param("unrestricted") boolean unrestricted,
                                      @Param("warehouseIds") List<Long> warehouseIds,
                                      @Param("updatedBy") String updatedBy,
                                      @Param("updatedAt") LocalDateTime updatedAt);
}
