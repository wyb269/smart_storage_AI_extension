package com.chinaunicom.smartstorage.admin.service.inter.doorAnomaly;

import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.AnomalyEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.DoorStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventProcessReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.entity.doorAnomaly.AiAlgorithmTask;

import java.math.BigDecimal;
import java.util.List;

/**
 * 仓门异动数据访问接口。
 *
 * <p>依据 AI_HANDOFF.md 第 23 节，Mapper 层负责
 * 仓门、摄像头、事件、证据和处理记录的数据访问。
 * 本接口即为该层的业务抽象。</p>
 *
 * <p><b>当前实现</b>：{@code InMemoryDoorAnomalyRepository}。
 * 真实数据库（{@code smart_storage_ai_extension}）建表与数据源切换
 * 完成后再提供基于 MyBatis-Plus 的实现，届时只需新增实现类，
 * 上层 Service 不变。</p>
 *
 * <p><b>P0-6 数据范围要求</b>：<b>所有</b>读取方法都必须接收
 * {@link AiVisionDataScope}，并在 SQL 中落实组织/区域过滤。
 * 详情查询与条件更新必须同时按事件标识与数据范围约束，
 * 禁止只按事件标识裸查。</p>
 *
 * <p><b>实现要求</b>：</p>
 * <ul>
 *   <li>处理事件必须使用<b>乐观锁</b>（event_version），
 *       版本不匹配时更新行数为 0，由 Service 判定为并发冲突。</li>
 *   <li>处理记录必须<b>追加写入</b>，不得覆盖历史意见（第 17.6 节）。</li>
 *   <li>不得用删除或覆盖算法结果的方式完成事件处理（第 23 节）。</li>
 *   <li>返回对象不得包含摄像头凭据或永久媒体地址（第 29.1 节）。</li>
 * </ul>
 *
 * @author 仓门异动功能
 */
public interface DoorAnomalyRepository {

    /**
     * 查询当前用户可见的仓门状态。
     *
     * @param scope 数据范围（组织/区域），不可为 null
     */
    List<DoorStatusRespDTO> listDoorStatus(AiVisionDataScope scope);

    /**
     * 查询最近事件，按发生时间倒序。
     *
     * @param scope 数据范围，不可为 null
     * @param limit 展示条数
     */
    List<AnomalyEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit);

    /**
     * 按条件分页查询事件。
     *
     * <p>排序由 SQL 层完成，默认按发生时间倒序（第 18.3 节）。
     * 查询必须叠加数据范围过滤。</p>
     *
     * @param scope 数据范围，不可为 null
     * @param query 查询条件
     * @return 当前页数据
     */
    List<AnomalyEventRespDTO> listEventByCondition(AiVisionDataScope scope, EventQueryReqDTO query);

    /**
     * 按条件查询全部事件（导出用，不分页）。
     *
     * @param scope 数据范围，不可为 null
     * @param query 查询条件
     * @param limit 条数上限
     */
    List<AnomalyEventRespDTO> listEventForExport(AiVisionDataScope scope, EventQueryReqDTO query, int limit);

    /**
     * 查询事件详情（<b>必须校验数据范围</b>）。
     *
     * @param scope 数据范围，不可为 null
     * @param eventId 事件标识
     * @return 详情；不存在或超出授权范围时返回 null
     */
    EventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId);

    /**
     * 查询事件的全部处理记录（追加式，按时间正序）。
     *
     * @param scope 数据范围，不可为 null
     * @param eventId 事件标识
     */
    List<ProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId);

    /**
     * 提交人工处理：乐观锁更新事件状态 + 追加一条处理记录。
     *
     * <p><b>P1-7：这两步必须在同一个事务里完成</b>，否则会出现
     * 「状态已变但无审计」——审计缺失正是本条要防的事故。
     * 因此状态更新与记录追加<b>不得</b>拆成两个接口方法由上层顺序调用：
     * 跨事务管理器时内层不会加入外层事务，两次调用就是两次独立提交。</p>
     *
     * <p><b>实现要求</b>：</p>
     * <ul>
     *   <li>必须使用乐观锁
     *       {@code UPDATE ... WHERE id = ? AND event_version = ? AND <数据范围条件>}；
     *       版本不匹配或超出授权范围时<b>不更新任何字段、不写记录</b>，返回 0。</li>
     *   <li>算法结论与算法任务状态<b>不得</b>被本次更新修改（第 23 节）。</li>
     *   <li>处理记录必须<b>追加写入</b>，不得覆盖历史（第 17.6 节）。</li>
     *   <li>事务必须绑定<b>本功能独立库</b>的事务管理器；该管理器仅在
     *       {@code ai-vision.datasource.enabled=true} 时存在，
     *       因此<b>不得</b>由上层按 Bean 名字声明事务（内存兜底模式下会
     *       在首次调用时抛 {@code NoSuchBeanDefinitionException}）。</li>
     * </ul>
     *
     * @param scope        数据范围，不可为 null
     * @param eventId      事件标识
     * @param eventVersion 用户读取到的事件版本号
     * @param statusBefore 处理前状态，写入审计记录的 status_before
     * @param req          处理请求（不含处理人）
     * @param processedBy  处理人，由 Service 从登录态获取
     * @return 受影响行数；1 表示成功，0 表示版本冲突或越权
     */
    int processEvent(AiVisionDataScope scope, String eventId, Integer eventVersion,
                     String statusBefore, EventProcessReqDTO req, String processedBy);

    /* ==================== 算法任务（契约 AI_VISION_ALGORITHM_DATA_INTERACTION.md §1） ====================
     * 以下方法支撑「重新识别」：受理时先落一行 QUEUED/RUNNING 任务，
     * 前端轮询时按需读回并用网关最新结果更新，成功后回写事件算法维度。
     * 所有读写都在数据范围内执行（P0-6）。 */

    /**
     * 落一行算法任务。实现负责生成并回填 {@code task.id}（雪花 / 内存自增）。
     *
     * @param task 待落库的任务（QUEUED/RUNNING，is_simulated=TRUE）
     */
    void insertAlgorithmTask(AiAlgorithmTask task);

    /**
     * 按算法任务编号（task_no）在数据范围内查任务。
     *
     * @return 越权或不存在返回 null
     */
    AiAlgorithmTask getAlgorithmTaskByTaskNo(String taskNo, AiVisionDataScope scope);

    /**
     * 按事件列算法任务，started_at 倒序，最多 limit 条。
     *
     * <p>供详情弹窗「算法任务」表与 listAlgorithmTasks 端点使用。</p>
     */
    java.util.List<AiAlgorithmTask> listAlgorithmTasksByEvent(String eventId,
                                                              AiVisionDataScope scope,
                                                              int limit);

    /**
     * 用网关最新结果更新任务行（状态/结论/置信度/模型/版本/耗时/检测框/错误码）。
     */
    void updateAlgorithmTask(AiAlgorithmTask task);

    /**
     * 「重新识别」成功后回写事件的算法维度。
     *
     * <p><b>只写算法相关列，绝不触碰 process_status</b>（第 23 节）；
     * {@code anomalyType} 为空时保留事件原值（NOT NULL）；
     * {@code algorithmResult} 仅在成功产出结论时写入，不得伪造 NORMAL。
     * 带数据范围（P0-6），越权返回 0。</p>
     *
     * @return 受影响行数
     */
    int writebackEventAlgorithm(AiVisionDataScope scope, String eventId, Long algorithmTaskId,
                                String algorithmStatus, String algorithmResult,
                                BigDecimal confidence, String anomalyType, String updatedBy);
}