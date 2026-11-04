package com.chinaunicom.smartstorage.admin.service.inter.beltMachine;

import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineMonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.entity.beltMachine.BeltMachineAlgorithmTask;

import java.math.BigDecimal;
import java.util.List;

/**
 * 皮带机下站人数据访问接口。
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
 * @author aiVision 功能族 · 皮带机下站人监测
 */
public interface BeltMachineRepository {

    List<BeltMachineMonitorPointRespDTO> listMonitorPoint(AiVisionDataScope scope);

    List<BeltMachineEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit);

    List<BeltMachineEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                       BeltMachineEventQueryReqDTO query);

    List<BeltMachineEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                     BeltMachineEventQueryReqDTO query, int limit);

    /**
     * 查询事件详情（必须校验数据范围）。
     *
     * @return 详情；不存在或超出授权范围时返回 null
     */
    BeltMachineEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId);

    /** 查询事件的全部处理记录（追加式，按时间正序） */
    List<BeltMachineProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId);

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
     * <p><b>比视频遮挡那一版多一个 {@code source} 参数</b>：基线要求
     * 「查看即标记」写 {@code source=SYSTEM}，与人工处理写 {@code WEB} 区分开，
     * 否则审计里分不清哪些结论来自人。遮挡那一版把它写死成 WEB，
     * 这里不再照抄。</p>
     *
     * @return 受影响行数；1 表示成功，0 表示版本冲突或越权
     */
    int processEvent(AiVisionDataScope scope, String eventId, Integer eventVersion,
                     String statusBefore, String processResult, String processOpinion,
                     String processedBy, String source);

    /* ==================== 算法任务（重新识别链路） ==================== */

    /**
     * 落一行算法任务；id 为空时由实现发号。
     *
     * @return 行标识（雪花或内存自增），供事件回写 algorithm_task_id 使用
     */
    long insertAlgorithmTask(BeltMachineAlgorithmTask task);

    /**
     * 按任务编号 + 数据范围查任务。
     *
     * @return 越权或不存在返回 null
     */
    BeltMachineAlgorithmTask getAlgorithmTaskByNo(AiVisionDataScope scope, String taskNo);

    /**
     * 更新任务的执行结果列（状态/结论/置信度/模型/版本/耗时/检测框/错误码）。
     *
     * <p>调用前必须已通过带范围的查询取回该行。</p>
     *
     * @return 受影响行数
     */
    int updateAlgorithmTask(BeltMachineAlgorithmTask task);

    /**
     * 查询事件的算法任务列表（开始时间倒序，最多 limit 条）。
     *
     * <p>实现必须自行校验事件在数据范围内，不得只依赖调用方先查过详情。</p>
     */
    List<BeltMachineAlgorithmTask> listAlgorithmTasks(AiVisionDataScope scope, String eventId, int limit);

    /**
     * 算法任务成功后回写事件的算法列与结论类型。
     *
     * <p><b>只写 algorithm_task_id / algorithm_status / algorithm_result /
     * confidence / conclusion_type</b>，不得触碰 process_* 与 event_version：
     * 机器重跑不构成人工结论。</p>
     *
     * @param conclusionType 任务给出的标准化结论类型；为空时保留事件原值
     * @return 受影响行数；0 表示事件越权或不存在
     */
    int writeBackEventAlgorithm(AiVisionDataScope scope, String eventId, Long taskId,
                                String algorithmStatus, String algorithmResult,
                                String conclusionType, BigDecimal confidence, String updatedBy);
}
