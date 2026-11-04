package com.chinaunicom.smartstorage.admin.service.inter.perimeterIntrusion;

import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.entity.perimeterIntrusion.AiPerimeterAlgorithmTask;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 周界入侵数据访问接口。
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
 * @author aiVision 功能族 · 周界入侵识别
 */
public interface PerimeterIntrusionRepository {

    List<MonitorPointStatusRespDTO> listMonitorPoint(AiVisionDataScope scope);

    List<PerimeterEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit);

    List<PerimeterEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                     PerimeterEventQueryReqDTO query);

    List<PerimeterEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                   PerimeterEventQueryReqDTO query, int limit);

    /**
     * 查询事件详情（必须校验数据范围）。
     *
     * @return 详情；不存在或超出授权范围时返回 null
     */
    PerimeterEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId);

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
     * @return 受影响行数；1 表示成功，0 表示版本冲突或越权
     */
    int processEvent(AiVisionDataScope scope, String eventId, Integer eventVersion,
                     String statusBefore, String processResult, String processOpinion,
                     String processedBy);

    /* ==================== 算法任务（契约 §1） ==================== */

    /**
     * 带数据范围取事件主记录，供「重新识别」前置校验。
     *
     * @return 越权或不存在返回 null
     */
    PerimeterEventRespDTO getEventSummary(AiVisionDataScope scope, String eventId);

    /**
     * 落一行算法任务（id 由实现侧发号后回填到入参实体并返回）。
     *
     * @return 生成的任务主键
     */
    Long insertAlgorithmTask(AiPerimeterAlgorithmTask task);

    /**
     * 按任务编号查任务（必须带数据范围：本表有 warehouse_id，
     * 只按编号裸查会让跨仓库猜测编号越权读到他人任务的结论）。
     *
     * @return 越权或不存在返回 null
     */
    AiPerimeterAlgorithmTask getAlgorithmTaskByNo(AiVisionDataScope scope, String taskNo);

    /** 查询某事件的算法任务，按 started_at 倒序，limit 上限由 Service 控制 */
    List<AiPerimeterAlgorithmTask> listAlgorithmTasks(AiVisionDataScope scope, String eventId, int limit);

    /**
     * 轮询后更新任务行的执行结果（状态/结论/置信度/模型/版本/耗时/检测框/错误码）。
     *
     * <p>按 taskNo 定位；调用前 Service 已用带范围的查询确认任务可见，
     * 与 {@link #processEvent} 的前置校验模式一致。</p>
     */
    int updateAlgorithmTaskResult(AiPerimeterAlgorithmTask task);

    /**
     * 算法成功后回写事件：algorithm_task_id / algorithm_status /
     * algorithm_result / confidence / intrusion_type。
     *
     * <p><b>绝不触碰 process_status 与 event_version</b>：算法结论与人工
     * 核实相互独立，重跑不得把「已处理」顶回「未核实」，也不得让
     * 在途的人工处理提交莫名撞上版本冲突。</p>
     *
     * @param intrusionType 算法给出的标准化类型；为 null 时事件保持原值
     *                      （事件列 NOT NULL，撤防外无法清空）
     */
    int writeBackEventAlgorithmResult(Long eventId, Long taskId, String algorithmStatus,
                                      String algorithmResult, BigDecimal confidence,
                                      String intrusionType, String updatedBy,
                                      LocalDateTime updatedAt);
}
