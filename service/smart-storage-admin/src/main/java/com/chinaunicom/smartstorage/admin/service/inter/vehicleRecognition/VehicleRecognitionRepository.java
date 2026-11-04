package com.chinaunicom.smartstorage.admin.service.inter.vehicleRecognition;

import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.MonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.TodayVehicleRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.entity.vehicleRecognition.VehicleRecognitionAlgorithmTask;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 车辆智能识别数据访问接口。
 *
 * <p><b>数据范围要求</b>：所有读取方法都必须接收 {@link AiVisionDataScope}，
 * 并在 SQL 中落实仓库过滤。详情查询与条件更新必须同时按记录标识与数据范围约束，
 * 禁止只按记录标识裸查。</p>
 *
 * <p><b>实现要求</b>：</p>
 * <ul>
 *   <li>核实记录必须使用乐观锁，版本不匹配时更新行数为 0，由 Service 判定并发冲突。</li>
 *   <li>核实审计必须追加写入，不得覆盖历史。</li>
 *   <li>不得用删除或覆盖算法结果的方式完成核实。</li>
 *   <li>返回对象不得包含摄像头凭据或永久媒体地址。</li>
 * </ul>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
public interface VehicleRecognitionRepository {

    List<TodayVehicleRespDTO> listTodayVehicle(AiVisionDataScope scope, int limit);

    List<MonitorPointRespDTO> listPoints(AiVisionDataScope scope);

    List<VehicleEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit);

    List<VehicleEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                   VehicleEventQueryReqDTO query);

    List<VehicleEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                                 VehicleEventQueryReqDTO query, int limit);

    /**
     * 查询识别记录详情（必须校验数据范围）。
     *
     * @return 详情；不存在或超出授权范围时返回 null
     */
    VehicleEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId);

    /** 查询记录的全部核实记录（追加式，按时间正序） */
    List<ProcessRecordRespDTO> listProcessRecords(AiVisionDataScope scope, String eventId);

    /**
     * 提交人工核实：乐观锁更新记录状态 + 追加一条核实审计。
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
                     String statusBefore, String verifyResult, String verifyOpinion,
                     String processedBy);

    /* ==================== 算法任务（重新识别） ==================== */

    /**
     * 取一条识别记录的算法任务上下文骨架。
     *
     * <p>返回的实体只填了 {@code eventId / pointId / cameraId / cameraCode /
     * warehouseId / regionId / orgId} 这些「属于谁」的列，状态与结果列留空，
     * 由 Service 受理算法任务时补齐。这样点位/仓库/行政区划一律以库里的记录为准，
     * 不接受前端拼装的归属信息。</p>
     *
     * @return 记录不存在或超出数据范围时返回 null
     */
    VehicleRecognitionAlgorithmTask findAlgorithmTaskContext(AiVisionDataScope scope, String eventId);

    /**
     * 落一行算法任务。
     *
     * <p>主键由实现方发号（雪花），与其余 {@code ai_vehicle_*} 表一致。</p>
     *
     * @return 落库后的任务主键
     */
    Long insertAlgorithmTask(VehicleRecognitionAlgorithmTask task);

    /**
     * 用算法适配层的最新输出更新任务行。
     *
     * <p>只覆盖非空字段（SQL 侧 COALESCE），避免适配层返回的兜底空对象
     * 把已落库的结论、置信度、检测框抹成空。</p>
     *
     * @return 受影响行数
     */
    int updateAlgorithmTask(AiVisionDataScope scope, VehicleRecognitionAlgorithmTask task);

    /** 按任务编号查询（带数据范围）；不存在或越权返回 null */
    VehicleRecognitionAlgorithmTask findAlgorithmTaskByTaskNo(AiVisionDataScope scope, String taskNo);

    /** 某条记录的历史算法任务，按开始时间倒序；实现方负责条数上限 */
    List<VehicleRecognitionAlgorithmTask> listAlgorithmTasksByEvent(AiVisionDataScope scope,
                                                                    String eventId, int limit);

    /**
     * 回写识别记录的算法字段。
     *
     * <p><b>只写 {@code algorithm_task_id / algorithm_status}，以及仅在成功时给的
     * {@code confidence}。</b>绝不允许触碰 {@code verify_*}（人工核实链路）与
     * 车牌/车型/方向等识别结果列——算法重跑是重新取证，不是改写业务结论。</p>
     *
     * <p>不递增 {@code event_version}：它是人工核实的乐观锁，重跑不该让
     * 并发进行中的核实误报「记录已被其他用户处理」。</p>
     *
     * @param confidence 仅成功态传入；失败/进行中传 null 表示保持原值
     * @return 受影响行数
     */
    int writeBackEventAlgorithmResult(AiVisionDataScope scope, String eventId, Long taskId,
                                      String executionStatus, BigDecimal confidence,
                                      String updatedBy, LocalDateTime updatedAt);
}
