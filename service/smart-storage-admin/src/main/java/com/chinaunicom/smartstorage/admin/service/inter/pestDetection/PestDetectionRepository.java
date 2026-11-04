package com.chinaunicom.smartstorage.admin.service.inter.pestDetection;

import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.MonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.algorithm.PestAlgorithmWriteBackDTO;
import com.chinaunicom.smartstorage.admin.entity.pestDetection.AiPestAlgorithmTask;

import java.util.List;

/**
 * 粮面虫害数据访问接口。
 *
 * <p><b>数据范围要求</b>：所有读取方法都必须接收 {@link AiVisionDataScope}，
 * 并在 SQL 中落实仓库过滤。详情查询与条件更新必须同时按记录标识与数据范围约束，
 * 禁止只按记录标识裸查。</p>
 *
 * <p><b>实现要求</b>：</p>
 * <ul>
 *   <li>处理记录必须使用乐观锁，版本不匹配时更新行数为 0，由 Service 判定并发冲突。</li>
 *   <li>处理审计必须追加写入，不得覆盖历史。</li>
 *   <li>不得用删除或覆盖算法结果的方式完成处理。</li>
 *   <li>返回对象不得包含摄像头凭据或永久媒体地址。</li>
 * </ul>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
public interface PestDetectionRepository {

    List<MonitorPointRespDTO> listMonitorPoint(AiVisionDataScope scope);

    List<PestEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit);

    List<PestEventRespDTO> listEventByCondition(AiVisionDataScope scope, PestEventQueryReqDTO query);

    List<PestEventRespDTO> listEventForExport(AiVisionDataScope scope, PestEventQueryReqDTO query, int limit);

    /**
     * 查询检测记录详情（必须校验数据范围）。
     *
     * @return 详情；不存在或超出授权范围时返回 null
     */
    PestEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId);

    /** 查询记录的全部处理审计（追加式，按时间正序） */
    List<ProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId);

    /**
     * 提交人工处理：乐观锁更新状态 + 追加一条处理审计，二者必须处于同一事务。
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

    /* ==================== 算法任务（「重新识别」链路） ==================== */

    /**
     * 落一行算法任务（QUEUED/RUNNING，{@code is_simulated=TRUE}）。
     *
     * <p>实现负责补齐主键 id（应用侧发号）与创建时间；同一事务内不做其它写入。</p>
     *
     * @return 落库后的任务行标识（供事件回写 algorithm_task_id 用）
     */
    Long insertAlgorithmTask(AiPestAlgorithmTask task);

    /**
     * 按任务编号查询算法任务（必须校验数据范围，按任务行自身 warehouse_id 下推）。
     *
     * @return 越权或不存在时返回 null
     */
    PestAlgorithmTaskRespDTO findAlgorithmTaskByTaskNo(AiVisionDataScope scope, String taskNo);

    /**
     * 查询某事件的历史算法任务，按 started_at 倒序。
     *
     * <p>范围校验先按事件归属仓库判定（调用方已保证事件在范围内），
     * 本方法只负责取任务行。</p>
     */
    List<PestAlgorithmTaskRespDTO> listAlgorithmTasks(AiVisionDataScope scope, String eventId, int limit);

    /**
     * 保存网关查询回来的任务最新状态；任务成功时同步回写事件算法列。
     *
     * <p><b>两个写入必须处于同一事务</b>（实现类内声明，绑定本功能独立库事务管理器），
     * 回写只动 algorithm_* 与本功能结论列，<b>不得触碰 process_status / event_version</b>。</p>
     *
     * @param task       已合并网关结果的任务实体（按 id 更新）
     * @param writeBack  事件回写载荷；任务未成功时传 null（只更新任务行）
     */
    void saveAlgorithmTaskResult(AiPestAlgorithmTask task, PestAlgorithmWriteBackDTO writeBack);
}
