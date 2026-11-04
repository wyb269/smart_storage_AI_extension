package com.chinaunicom.smartstorage.admin.service.inter.personBehavior;

import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.entity.personBehavior.AiPersonAlgorithmTask;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 人员行为监测数据访问接口。
 *
 * <p><b>数据范围要求</b>：所有读取方法都必须接收 {@link AiVisionDataScope}，
 * 并在 SQL 中落实仓库过滤。详情查询与条件更新必须同时按事件标识与数据范围约束，
 * 禁止只按事件标识裸查。</p>
 *
 * <p><b>实现要求</b>：</p>
 * <ul>
 *   <li>处理事件必须使用乐观锁，版本不匹配时更新行数为 0，由 Service 判定并发冲突。</li>
 *   <li>处理记录必须追加写入，不得覆盖历史。</li>
 *   <li>不得用删除或覆盖算法结果的方式完成事件处理。</li>
 *   <li>返回对象不得包含摄像头凭据或永久媒体地址。</li>
 * </ul>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
public interface PersonBehaviorRepository {

    List<MonitorPointStatusRespDTO> listMonitorPoint(AiVisionDataScope scope);

    List<PersonEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit);

    List<PersonEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                  PersonEventQueryReqDTO query);

    List<PersonEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                PersonEventQueryReqDTO query, int limit);

    /**
     * 查询事件详情（必须校验数据范围）。
     *
     * @return 详情；不存在或超出授权范围时返回 null
     */
    PersonEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId);

    /** 查询事件的全部处理记录（追加式，按时间正序） */
    List<ProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId);

    /**
     * 提交人工处理：乐观锁更新事件状态 + 追加一条处理记录。
     *
     * <p><b>这两步必须在同一个事务里完成</b>，否则会出现「状态已变但无审计」。
     * 因此不得拆成两个接口方法由上层顺序调用。</p>
     *
     * <p><b>事务必须声明在实现类上并绑定本功能独立库的事务管理器</b>；
     * 该管理器仅在 {@code ai-vision.datasource.enabled=true} 时存在，
     * 因此不得由 Service 按 Bean 名字声明事务（内存兜底模式下会抛
     * {@code NoSuchBeanDefinitionException}）。</p>
     *
     * @param source 审计来源，取值见 {@code PersonBehaviorConstant.PROCESS_SOURCE_*}。
     *               由 Service 按调用入口决定，实现层不得自行写死：
     *               「查看即标记」记 SYSTEM、页面人工处理记 WEB，
     *               两者混成一个值就没法把自动记录从人工结论里剔出去。
     * @return 受影响行数；1 表示成功，0 表示版本冲突或越权
     */
    int processEvent(AiVisionDataScope scope, String eventId, Integer eventVersion,
                     String statusBefore, String processResult, String processOpinion,
                     String processedBy, String source);

    /* ==================== 算法任务（重新识别） ==================== */

    /**
     * 落一行算法任务。
     *
     * <p>实现负责补齐主键（真实库为雪花发号）；调用方不得传库内已有 id。</p>
     */
    void insertAlgorithmTask(AiPersonAlgorithmTask task);

    /** 按任务编号更新执行状态与结论字段（轮询取到最新输出后落库） */
    void updateAlgorithmTask(AiPersonAlgorithmTask task);

    /**
     * 按任务编号查询算法任务（带数据范围）。
     *
     * <p>越权与不存在一律返回 null，与事件详情的口径一致。</p>
     */
    AiPersonAlgorithmTask findAlgorithmTaskByTaskNo(AiVisionDataScope scope, String taskNo);

    /** 查询某事件的算法任务，按开始时间（缺省取创建时间）倒序 */
    List<AiPersonAlgorithmTask> listAlgorithmTasksByEventId(AiVisionDataScope scope,
                                                            String eventId, int limit);

    /**
     * 算法任务成功后回写事件。
     *
     * <p>只写 algorithm_task_id / algorithm_status / algorithm_result /
     * confidence / behavior_type（behaviorType 为空时保留原值），
     * <b>绝不触碰 process_status 与 event_version</b>：
     * 算法结论与人工结论两条链路互不覆盖，乐观锁版本也不该被系统行为推进。</p>
     *
     * @return 受影响行数；0 表示事件不存在或超出数据范围
     */
    int backfillEventAlgorithmResult(AiVisionDataScope scope, Long eventId, Long taskId,
                                     String algorithmStatus, String algorithmResult,
                                     BigDecimal confidence, String behaviorType,
                                     String updatedBy, LocalDateTime updatedAt);
}
