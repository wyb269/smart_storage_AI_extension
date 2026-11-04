package com.chinaunicom.smartstorage.admin.mapper.aivision.vehicleRecognition;

import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.MonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.TodayVehicleRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.entity.vehicleRecognition.VehicleRecognitionAlgorithmTask;
import com.chinaunicom.smartstorage.admin.entity.vehicleRecognition.VehicleRecognitionEvent;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 车辆识别数据访问 Mapper，覆盖识别记录 / 识别点位 / 证据 / 审计 / 算法任务五张表。
 *
 * <p><b>关键实现要求</b>：</p>
 * <ol>
 *   <li>每个查询都必须带仓库数据范围三态下推，非全域且无授权仓库时恒假。</li>
 *   <li>核实使用乐观锁，且<b>不得</b>触碰 algorithm_* 与识别结果列（车牌/车型/颜色/方向）。</li>
 *   <li>算法任务回写反过来：<b>不得</b>触碰 verify_* 与识别结果列，也不得动 event_version。</li>
 *   <li>默认按识别时间倒序。</li>
 *   <li>时间列在 SQL 侧 {@code to_char} 格式化，排序用裸列。</li>
 * </ol>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
public interface VehicleRecognitionMapper extends MPJBaseMapper<VehicleRecognitionEvent> {

    /**
     * 查询今日识别车辆（左侧列表）。
     *
     * <p>「今日」以识别时间 {@code recognized_at::date = CURRENT_DATE} 判定，
     * 按识别时间倒序，受数据范围约束。</p>
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     * @param limit        展示条数
     */
    List<TodayVehicleRespDTO> listTodayVehicle(@Param("unrestricted") boolean unrestricted,
                                               @Param("warehouseIds") List<Long> warehouseIds,
                                               @Param("limit") int limit);

    /**
     * 查询当前用户可见的识别点位（供 Service 定位摄像头、判定在线）。
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     */
    List<MonitorPointRespDTO> listPoints(@Param("unrestricted") boolean unrestricted,
                                         @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询最近识别记录，按识别时间倒序。
     *
     * @param unrestricted 是否具备全域数据权限
     * @param warehouseIds 授权仓库标识集合，可为空
     * @param limit        展示条数
     */
    List<VehicleEventRespDTO> listRecentEvent(@Param("unrestricted") boolean unrestricted,
                                              @Param("warehouseIds") List<Long> warehouseIds,
                                              @Param("limit") int limit);

    /**
     * 按条件查询识别记录（分页由 {@code @QueryByPage} 切面完成）。
     *
     * <p>{@code @Param("query")} 不可省略：XML 内以 {@code query.xxx} 引用属性，
     * 单参数不加注解时 MyBatis 会把实体作为 OGNL 根对象，
     * 导致 {@code query.plateNo} 解析不到而整个 where 退化。</p>
     */
    List<VehicleEventRespDTO> listEventByCondition(@Param("query") VehicleEventQueryReqDTO query);

    /**
     * 按条件查询识别记录（导出用，不分页，受条数上限约束）。
     */
    List<VehicleEventRespDTO> listEventForExport(@Param("query") VehicleEventQueryReqDTO query,
                                                 @Param("limit") int limit);

    /**
     * 带数据范围查询识别记录主记录，用于详情前置校验与取 event_no。
     *
     * @return 越权或不存在时返回 null，不区分两种情况以避免泄露对象存在性
     */
    VehicleEventRespDTO getEventSummaryInScope(@Param("eventId") Long eventId,
                                               @Param("unrestricted") boolean unrestricted,
                                               @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询识别记录详情（带数据范围校验）。
     */
    VehicleEventDetailRespDTO getEventDetailInScope(@Param("eventId") Long eventId,
                                                    @Param("unrestricted") boolean unrestricted,
                                                    @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询某记录的全部证据。短时地址由 Service 经 MediaGateway 签发。
     */
    List<VehicleEvidenceRespDTO> listEvidenceByEventId(@Param("eventId") Long eventId);

    /**
     * 查询某记录的全部核实记录，按时间正序。
     */
    List<ProcessRecordRespDTO> listProcessRecords(@Param("eventId") Long eventId);

    /**
     * 以乐观锁方式提交核实结果。
     *
     * @return 受影响行数；1 成功，0 表示版本冲突或状态不允许
     */
    int updateVerifyWithVersion(@Param("eventId") Long eventId,
                                @Param("eventVersion") Integer eventVersion,
                                @Param("verifyStatus") String verifyStatus,
                                @Param("processedBy") String processedBy,
                                @Param("verifyOpinion") String verifyOpinion,
                                @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 追加一条核实审计记录。
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

    /* ==================== 算法任务 ai_vehicle_algorithm_task ==================== */

    /**
     * 取识别记录的算法任务归属骨架（点位/仓库/行政区划/摄像头）。
     *
     * <p>归属信息只能由库里记录反推，不能让前端拼出来后原样落库，
     * 否则伪造 warehouse_id 就能把任务写到别人仓库下。带同样的数据范围条件。</p>
     *
     * @return 越权或记录不存在时返回 null
     */
    VehicleRecognitionAlgorithmTask selectAlgorithmTaskContext(@Param("eventId") Long eventId,
                                                              @Param("unrestricted") boolean unrestricted,
                                                              @Param("warehouseIds") List<Long> warehouseIds);

    /** 落一行算法任务（id 由应用侧发号） */
    int insertAlgorithmTask(VehicleRecognitionAlgorithmTask task);

    /**
     * 用算法适配层输出更新任务行。
     *
     * <p>结果列一律 {@code COALESCE(新值, 旧值)}：适配层的兜底返回是空对象，
     * 直接覆盖会把已有结论与置信度抹掉。</p>
     */
    int updateAlgorithmTask(@Param("task") VehicleRecognitionAlgorithmTask task,
                            @Param("unrestricted") boolean unrestricted,
                            @Param("warehouseIds") List<Long> warehouseIds);

    /** 按 task_no 查询任务（带数据范围）；task_no 有唯一约束 */
    VehicleRecognitionAlgorithmTask selectAlgorithmTaskByTaskNo(@Param("taskNo") String taskNo,
                                                                @Param("unrestricted") boolean unrestricted,
                                                                @Param("warehouseIds") List<Long> warehouseIds);

    /** 某条记录的历史算法任务，按 started_at 倒序 */
    List<VehicleRecognitionAlgorithmTask> selectAlgorithmTasksByEvent(@Param("eventId") Long eventId,
                                                                      @Param("unrestricted") boolean unrestricted,
                                                                      @Param("warehouseIds") List<Long> warehouseIds,
                                                                      @Param("limit") int limit);

    /**
     * 回写识别记录的算法字段。
     *
     * <p><b>只动 {@code algorithm_task_id / algorithm_status / confidence / updated_*}；
     * 不得出现在 {@code verify_*}、车牌、车型、方向、{@code event_version} 上。</b>
     * confidence 用 COALESCE，失败/进行中态保持原值而不是清空。</p>
     */
    int writeBackEventAlgorithm(@Param("eventId") Long eventId,
                                @Param("taskId") Long taskId,
                                @Param("executionStatus") String executionStatus,
                                @Param("confidence") BigDecimal confidence,
                                @Param("updatedBy") String updatedBy,
                                @Param("updatedAt") LocalDateTime updatedAt,
                                @Param("unrestricted") boolean unrestricted,
                                @Param("warehouseIds") List<Long> warehouseIds);
}
