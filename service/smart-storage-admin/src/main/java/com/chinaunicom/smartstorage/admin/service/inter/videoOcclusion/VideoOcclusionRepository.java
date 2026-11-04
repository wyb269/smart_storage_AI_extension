package com.chinaunicom.smartstorage.admin.service.inter.videoOcclusion;

import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.CameraStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm.OcclusionAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.entity.videoOcclusion.AiOcclusionAlgorithmTask;

import java.math.BigDecimal;
import java.util.List;

/**
 * 视频遮挡数据访问接口。
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
 * @author aiVision 功能族 · 视频遮挡检测
 */
public interface VideoOcclusionRepository {

    List<CameraStatusRespDTO> listCameraStatus(AiVisionDataScope scope);

    List<OcclusionEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit);

    List<OcclusionEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                     OcclusionEventQueryReqDTO query);

    List<OcclusionEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                   OcclusionEventQueryReqDTO query, int limit);

    /**
     * 查询事件详情（必须校验数据范围）。
     *
     * @return 详情；不存在或超出授权范围时返回 null
     */
    OcclusionEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId);

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

    /* ==================== 算法任务（契约 §1「重新识别」） ==================== */

    /**
     * 落一行算法任务（QUEUED/RUNNING，is_simulated 由 Service 显式置 TRUE）。
     *
     * <p>id 为空时由实现发号（真实库=雪花，内存=自增），与既有表保持一致。</p>
     *
     * @return 落库后的任务主键字符串
     */
    String insertAlgorithmTask(AiOcclusionAlgorithmTask task);

    /**
     * 按任务编号查询（带数据范围）。
     *
     * @return 越权或不存在时返回 null
     */
    OcclusionAlgorithmTaskRespDTO findAlgorithmTaskByTaskNo(AiVisionDataScope scope, String taskNo);

    /**
     * 用算法侧最新输出回填任务行。
     *
     * <p>前置条件：调用方已用 {@link #findAlgorithmTaskByTaskNo} 完成数据范围校验。
     * 更新语句覆盖全部结果列，因此无需先按主键取整行。</p>
     *
     * @return 1 表示更新成功；0 表示任务在这期间已被删除（按失败处理）
     */
    int updateAlgorithmTask(AiOcclusionAlgorithmTask task);

    /**
     * 查询某事件的算法任务，按 started_at 倒序、受 limit 约束（带数据范围）。
     */
    List<OcclusionAlgorithmTaskRespDTO> listAlgorithmTasksByEvent(AiVisionDataScope scope,
                                                                  String eventId, int limit);

    /**
     * 算法成功后回写事件的 algorithm_task_id / algorithm_status /
     * algorithm_result / confidence。
     *
     * <p><b>不得触碰 process_status 与 event_version</b>：重新取证不是人工处理。</p>
     *
     * @return 受影响行数；0 表示越权或事件已不存在
     */
    int writeBackEventAlgorithmResult(AiVisionDataScope scope, String eventId, Long taskId,
                                      String algorithmStatus, String algorithmResult,
                                      BigDecimal confidence, String updatedBy);
}
