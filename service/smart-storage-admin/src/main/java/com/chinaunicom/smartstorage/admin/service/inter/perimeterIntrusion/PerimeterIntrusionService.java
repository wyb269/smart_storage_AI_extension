package com.chinaunicom.smartstorage.admin.service.inter.perimeterIntrusion;

import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterAlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.PerimeterExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

/**
 * 周界入侵识别主业务服务。
 *
 * <p>Controller 只做参数校验与统一响应，本接口负责业务状态组合、
 * 事件查询、人工处理与并发控制。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
public interface PerimeterIntrusionService {

    /** 查询当前用户可见的防区段状态，必须按登录态做数据范围过滤 */
    List<MonitorPointStatusRespDTO> listMonitorPoint();

    /**
     * 获取指定防区段绑定摄像头的短时播放信息。
     *
     * <p>返回的 playableUrl 必须是短时签名地址，
     * 且不得包含摄像头账号、密码或永久 Token。</p>
     */
    ActionResult<StreamInfoRespDTO, String> getStreamInfo(String pointId);

    /** 查询首页最近事件，按发生时间倒序 */
    List<PerimeterEventRespDTO> listRecentEvent(int limit);

    /**
     * 历史记录服务端分页查询。
     *
     * <p>分页由 {@code @QueryByPage} 切面完成，本方法只返回当前页数据。
     * 查询条件含 {@code zone} 防区筛选。</p>
     */
    List<PerimeterEventRespDTO> listEventByCondition(PerimeterEventQueryReqDTO query);

    /** 查询事件详情、证据与处理记录 */
    PerimeterEventDetailRespDTO getEventDetail(String eventId);

    /**
     * 人工核实事件。
     *
     * <p>使用事件版本号做乐观锁并发控制；处理人从登录态获取，
     * 不信任前端传入。必须已认证且事件在本用户数据范围内。</p>
     */
    ActionResult<String, String> processEvent(String eventId, String processResult,
                                              String processOpinion, Integer eventVersion);

    /**
     * 查看事件后自动标记为已处理。
     *
     * <p>产品口径与遮挡那一版一致：点开详情即视为已核实。
     * 仅当当前为 PENDING 时写入，重复调用无副作用。</p>
     *
     * <p>刻意做成独立接口而不是让 {@code getEventDetail} 顺带写库：
     * 读接口一旦带副作用，就会在处理链路与轮询路径上产生意外写入。</p>
     */
    ActionResult<String, String> markEventViewed(String eventId);

    /** 按查询条件查询导出数据（不分页，受服务端条数上限约束） */
    List<PerimeterEventRespDTO> listEventByConditionForExport(PerimeterExportReqDTO exportReqDTO);

    /* ==================== 算法任务（契约 §1「重新识别」三端点） ==================== */

    /**
     * 对指定事件重新提交算法识别任务。
     *
     * <p>流程（AI_HANDOFF 第 24.1 节）：校验事件在本用户数据范围内 →
     * 校验所属防区段<b>处于布防状态</b>（撤防段返回 POINT_DISARMED 业务错误，
     * 撤防期间不做识别）→ 组装统一算法输入 → {@code gateway.submitTask()}
     * → 落一行 QUEUED 任务（{@code is_simulated=TRUE}）→ <b>立即返回</b>，
     * 绝不在请求线程里等算法结果。</p>
     */
    PerimeterAlgorithmRerunRespDTO rerunAlgorithm(String eventId);

    /**
     * 查询算法任务，按需向网关取最新进度并落库。
     *
     * <p>先查库；任务未进终态时调 {@code gateway.queryTask} 取最新状态，
     * 更新任务的执行状态/结论/置信度/模型/版本/耗时/检测框/错误码；
     * 成功时回写事件的 algorithm_task_id、algorithm_status、confidence、
     * intrusion_type（仅在算法给出类型时），
     * 但<b>不触碰 process_status</b>（人工结论与算法结论相互独立）。</p>
     */
    PerimeterAlgorithmTaskRespDTO getAlgorithmTask(String taskNo);

    /** 查询指定事件的算法任务列表，按开始时间倒序（详情弹窗兜底数据源） */
    List<PerimeterAlgorithmTaskRespDTO> listAlgorithmTasks(String eventId);
}
