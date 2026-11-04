package com.chinaunicom.smartstorage.admin.mapper.aivision.personBehavior;

import com.chinaunicom.smartstorage.admin.dto.personBehavior.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.entity.personBehavior.PersonBehaviorEvent;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 人员行为监测数据访问 Mapper，覆盖事件 / 监测区域 / 证据 / 审计四张表。
 *
 * <p><b>关键实现要求</b>：</p>
 * <ol>
 *   <li>每个查询都必须带仓库数据范围三态下推，非全域且无授权仓库时恒假。</li>
 *   <li>人工处理使用乐观锁，且<b>不得</b>触碰 algorithm_* 列。</li>
 *   <li>默认按发生时间倒序。</li>
 *   <li>时间列在 SQL 侧 {@code to_char} 格式化，排序用裸列。</li>
 * </ol>
 *
 * <p>单参数方法同样必须写 {@code @Param}：XML 里以 {@code query.xxx} 形式引用属性时，
 * 缺注解会让 MyBatis 把实体当 OGNL 根对象，整个 where 条件静默退化。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
public interface PersonBehaviorMapper extends MPJBaseMapper<PersonBehaviorEvent> {

    /**
     * 查询当前用户可见的监测区域状态。
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     */
    List<MonitorPointStatusRespDTO> listMonitorPoint(@Param("unrestricted") boolean unrestricted,
                                                     @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询最近人员行为事件，按发生时间倒序。
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     * @param limit        展示条数
     */
    List<PersonEventRespDTO> listRecentEvent(@Param("unrestricted") boolean unrestricted,
                                             @Param("warehouseIds") List<Long> warehouseIds,
                                             @Param("limit") int limit);

    /**
     * 按条件查询事件（分页由 {@code @QueryByPage} 切面完成）。
     */
    List<PersonEventRespDTO> listEventByCondition(@Param("query") PersonEventQueryReqDTO query);

    /**
     * 按条件查询事件（导出用，不分页，受条数上限约束）。
     */
    List<PersonEventRespDTO> listEventForExport(@Param("query") PersonEventQueryReqDTO query,
                                                @Param("limit") int limit);

    /**
     * 带数据范围查询事件主记录，用于详情前置校验与取 event_no。
     *
     * @return 越权或不存在时返回 null，不区分两种情况以避免泄露对象存在性
     */
    PersonEventRespDTO getEventSummaryInScope(@Param("eventId") Long eventId,
                                              @Param("unrestricted") boolean unrestricted,
                                              @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询事件详情（带数据范围校验）。
     */
    PersonEventDetailRespDTO getEventDetailInScope(@Param("eventId") Long eventId,
                                                   @Param("unrestricted") boolean unrestricted,
                                                   @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询某事件的全部证据。短时地址由 Service 经 MediaGateway 签发。
     */
    List<PersonEvidenceRespDTO> listEvidenceByEventId(@Param("eventId") Long eventId);

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

    /**
     * 算法任务成功后回写事件的算法侧字段。
     *
     * <p>只写 algorithm_task_id / algorithm_status / algorithm_result /
     * confidence / behavior_type（behaviorType 为空时保留原值）与 updated_*；
     * <b>不触碰 process_status、process_* 与 event_version</b>。
     * 数据范围由调用方先用 getEventSummaryInScope 校验，
     * 与 updateProcessWithVersion 的口径一致。</p>
     */
    int updateAlgorithmBackfill(@Param("eventId") Long eventId,
                                @Param("taskId") Long taskId,
                                @Param("algorithmStatus") String algorithmStatus,
                                @Param("algorithmResult") String algorithmResult,
                                @Param("confidence") java.math.BigDecimal confidence,
                                @Param("behaviorType") String behaviorType,
                                @Param("updatedBy") String updatedBy,
                                @Param("updatedAt") LocalDateTime updatedAt);
}
