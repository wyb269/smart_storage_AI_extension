package com.chinaunicom.smartstorage.admin.service.inter.groundWater;

import com.chinaunicom.smartstorage.admin.dto.aivision.AiVisionDataScope;
import com.chinaunicom.smartstorage.admin.dto.groundWater.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventRespDTO;
import com.chinaunicom.smartstorage.admin.entity.groundWater.AiWaterAlgorithmTask;

import java.util.List;

/**
 * 地面积水数据访问接口。
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
 * @author aiVision 功能族 · 地面积水监测
 */
public interface GroundWaterRepository {

    List<MonitorPointStatusRespDTO> listMonitorPoint(AiVisionDataScope scope);

    List<WaterEventRespDTO> listRecentEvent(AiVisionDataScope scope, int limit);

    List<WaterEventRespDTO> listEventByCondition(AiVisionDataScope scope,
                                                 WaterEventQueryReqDTO query);

    List<WaterEventRespDTO> listEventForExport(AiVisionDataScope scope,
                                               WaterEventQueryReqDTO query, int limit);

    /**
     * 查询事件详情（必须校验数据范围）。
     *
     * @return 详情；不存在或超出授权范围时返回 null
     */
    WaterEventDetailRespDTO getEventDetail(AiVisionDataScope scope, String eventId);

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
     * <p><b>source 由调用方显式给出而不是仓储内部猜</b>：
     * 「查看即标记」写 SYSTEM、页面人工处理写 WEB，两者审计含义完全不同。
     * 若靠处理意见文案反推来源，改一句文案就会把自动记录伪装成人工结论。</p>
     *
     * @return 受影响行数；1 表示成功，0 表示版本冲突或越权
     */
    int processEvent(AiVisionDataScope scope, String eventId, Integer eventVersion,
                     String statusBefore, String processResult, String processOpinion,
                     String processedBy, String source);

    /* ==================== 算法任务（「重新识别」链路） ==================== */

    /**
     * 落一行算法任务（提交受理时的 QUEUED 行）。
     *
     * <p>主键由实现侧发号（MyBatis 用雪花，与其余 ai_water_* 表一致），
     * 回填到入参实体的 {@code id} 后返回同一实体。</p>
     *
     * @return 带主键的任务实体
     */
    AiWaterAlgorithmTask insertAlgorithmTask(AiWaterAlgorithmTask task);

    /**
     * 按 task_no 查询任务（必须校验数据范围）。
     *
     * <p>{@code task_no} 是算法侧唯一句柄：Mock 网关的 taskId 直接作为 task_no 落库，
     * 轮询端点据此查库。</p>
     *
     * @return 越权或不存在时返回 null，与事件详情同口径
     */
    AiWaterAlgorithmTask getAlgorithmTaskByTaskNo(String taskNo, AiVisionDataScope scope);

    /**
     * 持久化算法任务的最新产出；事件需要回填时（{@code writeBackEvent=true}）
     * 两条 UPDATE 必须处于同一事务，否则会出现「任务已 SUCCEEDED 但事件还挂着旧状态」。
     *
     * <p>事件回填<b>只写算法侧字段</b>（algorithm_task_id / algorithm_status /
     * algorithm_result / confidence / water_type），
     * <b>绝不触碰 process_status 与 event_version</b> ——
     * 算法重跑不产生人工结论，也不该搅动人工处理的乐观锁。</p>
     *
     * @param task            已填充最新产出字段的任务实体（按 taskNo 定位）
     * @param eventId         待回填的事件标识；null 表示不回填
     * @param writeBackEvent  是否把任务结论同步回事件行
     * @param scope           数据范围；写路径必须已认证且在范围内
     * @param updatedBy       更新人（登录态用户名）
     * @return 任务行的更新行数
     */
    int saveAlgorithmTaskOutcome(AiWaterAlgorithmTask task, String eventId,
                                 boolean writeBackEvent, AiVisionDataScope scope,
                                 String updatedBy);

    /**
     * 查询某事件的算法任务列表，按 started_at 倒序。
     *
     * <p>事件维度的范围校验由调用方先行完成（getEventDetail / getEventSummaryInScope），
     * 本方法再按任务行自身的 warehouse_id 叠加一层范围条件。</p>
     */
    List<AiWaterAlgorithmTask> listAlgorithmTasksByEventId(AiVisionDataScope scope,
                                                           String eventId, int limit);
}
