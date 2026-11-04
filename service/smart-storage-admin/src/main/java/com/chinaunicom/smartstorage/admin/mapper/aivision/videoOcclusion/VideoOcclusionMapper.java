package com.chinaunicom.smartstorage.admin.mapper.aivision.videoOcclusion;

import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.CameraStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm.OcclusionAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.entity.videoOcclusion.AiOcclusionAlgorithmTask;
import com.chinaunicom.smartstorage.admin.entity.videoOcclusion.VideoOcclusionEvent;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频遮挡数据访问 Mapper，覆盖遮挡事件 / 摄像头配置 / 证据 / 审计四张表。
 *
 * <p><b>关键实现要求</b>：</p>
 * <ol>
 *   <li>每个查询都必须带仓库数据范围三态下推，非全域且无授权仓库时恒假。</li>
 *   <li>人工处理使用乐观锁，且<b>不得</b>触碰 algorithm_* 列。</li>
 *   <li>默认按发生时间倒序。</li>
 *   <li>时间列在 SQL 侧 {@code to_char} 格式化，排序用裸列。</li>
 * </ol>
 *
 * @author aiVision 功能族 · 视频遮挡检测
 */
public interface VideoOcclusionMapper extends MPJBaseMapper<VideoOcclusionEvent> {

    /**
     * 查询当前用户可见的摄像头状态。
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     */
    List<CameraStatusRespDTO> listCameraStatus(@Param("unrestricted") boolean unrestricted,
                                               @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询最近遮挡事件，按发生时间倒序。
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     * @param limit        展示条数
     */
    List<OcclusionEventRespDTO> listRecentEvent(@Param("unrestricted") boolean unrestricted,
                                                @Param("warehouseIds") List<Long> warehouseIds,
                                                @Param("limit") int limit);

    /**
     * 按条件查询事件（分页由 {@code @QueryByPage} 切面完成）。
     *
     * <p>{@code @Param("query")} 不可省略：XML 内以 {@code query.xxx} 引用属性，
     * 单参数不加注解时 MyBatis 会把实体作为 OGNL 根对象，
     * 导致 {@code query.eventNo} 解析不到而整个 where 退化。</p>
     */
    List<OcclusionEventRespDTO> listEventByCondition(@Param("query") OcclusionEventQueryReqDTO query);

    /**
     * 按条件查询事件（导出用，不分页，受条数上限约束）。
     */
    List<OcclusionEventRespDTO> listEventForExport(@Param("query") OcclusionEventQueryReqDTO query,
                                                   @Param("limit") int limit);

    /**
     * 带数据范围查询事件主记录，用于详情前置校验与取 event_no。
     *
     * @return 越权或不存在时返回 null，不区分两种情况以避免泄露对象存在性
     */
    OcclusionEventRespDTO getEventSummaryInScope(@Param("eventId") Long eventId,
                                                 @Param("unrestricted") boolean unrestricted,
                                                 @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询事件详情（带数据范围校验）。
     */
    OcclusionEventDetailRespDTO getEventDetailInScope(@Param("eventId") Long eventId,
                                                      @Param("unrestricted") boolean unrestricted,
                                                      @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询某事件的全部证据。短时地址由 Service 经 MediaGateway 签发。
     */
    List<OcclusionEvidenceRespDTO> listEvidenceByEventId(@Param("eventId") Long eventId);

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

    /* ==================== 算法任务（契约 §1「重新识别」） ==================== */

    /**
     * 落一行算法任务（提交即写 QUEUED/RUNNING，结果由查询接口回填）。
     *
     * <p>单参数实体不加 {@code @Param}：MyBatis 以实体为 OGNL 根对象，
     * XML 内直接 {@code #{taskNo}} 等属性名取值。</p>
     */
    int insertAlgorithmTask(AiOcclusionAlgorithmTask task);

    /**
     * 按任务编号查询算法任务（带数据范围，与事件侧同一套三态下推）。
     *
     * @return 越权或不存在时返回 null，不区分两种情况以避免泄露对象存在性
     */
    OcclusionAlgorithmTaskRespDTO selectAlgorithmTaskByTaskNo(@Param("taskNo") String taskNo,
                                                              @Param("unrestricted") boolean unrestricted,
                                                              @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询某事件的全部算法任务，按 started_at 倒序（详情弹窗任务表与轮询刷新共用）。
     */
    List<OcclusionAlgorithmTaskRespDTO> listAlgorithmTasksByEvent(@Param("eventId") Long eventId,
                                                                  @Param("unrestricted") boolean unrestricted,
                                                                  @Param("warehouseIds") List<Long> warehouseIds,
                                                                  @Param("limit") int limit);

    /**
     * 用算法侧最新输出回填任务行（状态/结论/置信度/模型/版本/耗时/检测框/错误码）。
     *
     * <p>只按主键更新：调用前必须已经过
     * {@link #selectAlgorithmTaskByTaskNo} 的数据范围校验。</p>
     */
    int updateAlgorithmTaskResult(AiOcclusionAlgorithmTask task);

    /**
     * 算法成功后回写事件的 algorithm_task_id / algorithm_status /
     * algorithm_result / confidence 四列。
     *
     * <p><b>不得触碰 process_status、processed_* 与 event_version</b>：
     * 算法重新取证不是人工处理，也不得让在途的人工处理乐观锁失效。
     * WHERE 同样带数据范围下推，写路径不接受裸事件 ID。</p>
     */
    int updateEventAlgorithmResult(@Param("eventId") Long eventId,
                                   @Param("taskId") Long taskId,
                                   @Param("algorithmStatus") String algorithmStatus,
                                   @Param("algorithmResult") String algorithmResult,
                                   @Param("confidence") BigDecimal confidence,
                                   @Param("updatedBy") String updatedBy,
                                   @Param("updatedAt") LocalDateTime updatedAt,
                                   @Param("unrestricted") boolean unrestricted,
                                   @Param("warehouseIds") List<Long> warehouseIds);
}
