package com.chinaunicom.smartstorage.admin.service.inter.personBehavior;

import com.chinaunicom.smartstorage.admin.dto.personBehavior.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.PersonExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.algorithm.PersonAlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.algorithm.PersonAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

/**
 * 人员行为监测主业务服务。
 *
 * <p>Controller 只做参数校验与统一响应，本接口负责业务状态组合、
 * 事件查询、人工处理与并发控制。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
public interface PersonBehaviorService {

    /** 查询当前用户可见的监测区域状态，必须按登录态做数据范围过滤 */
    List<MonitorPointStatusRespDTO> listMonitorPoint();

    /**
     * 获取指定监测区域内绑定摄像头的短时播放信息。
     *
     * <p>返回的 playableUrl 必须是短时签名地址，
     * 且不得包含摄像头账号、密码或永久 Token。</p>
     */
    ActionResult<StreamInfoRespDTO, String> getStreamInfo(String pointId);

    /** 查询首页最近事件，按发生时间倒序 */
    List<PersonEventRespDTO> listRecentEvent(int limit);

    /**
     * 历史记录服务端分页查询。
     *
     * <p>分页由 {@code @QueryByPage} 切面完成，本方法只返回当前页数据。</p>
     */
    List<PersonEventRespDTO> listEventByCondition(PersonEventQueryReqDTO query);

    /** 查询事件详情、证据与处理记录 */
    PersonEventDetailRespDTO getEventDetail(String eventId);

    /**
     * 人工核实事件。
     *
     * <p>使用事件版本号做乐观锁并发控制；处理人从登录态获取，
     * 不信任前端传入。必须已认证且事件在本用户数据范围内。</p>
     *
     * <p><b>本期 Controller 不暴露此方法</b>（产品口径为「查看即标记」），
     * 保留实现是为了后续开启人工处理入口时无需改数据链路。</p>
     */
    ActionResult<String, String> processEvent(String eventId, String processResult,
                                              String processOpinion, Integer eventVersion);

    /**
     * 查看事件后自动标记为已处理。
     *
     * <p>仅当当前为 PENDING（未核实）时写入 DONE，重复调用无副作用。
     * PROCESSING（处理中）表示已有人工介入，不被查看动作抢着终结。</p>
     *
     * <p>刻意做成独立接口而不是让 {@code getEventDetail} 顺带写库：
     * 读接口一旦带副作用，就会在处理链路与轮询路径上产生意外写入。</p>
     */
    ActionResult<String, String> markEventViewed(String eventId);

    /** 按查询条件查询导出数据（不分页，受服务端条数上限约束） */
    List<PersonEventRespDTO> listEventByConditionForExport(PersonExportReqDTO exportReqDTO);

    /* ==================== 算法任务（重新识别） ==================== */

    /**
     * 对指定事件重新发起算法识别。
     *
     * <p>契约要求「提交 → 立即返回 taskId → 前端轮询」：
     * 本方法只做校验、组装输入、受理与落一行 QUEUED 任务，
     * <b>绝不在请求里等待算法完成</b>。</p>
     *
     * <p>结论由算法给出（当前为模拟结果，simulated=true），
     * 不产生人工结论，因此权限用 VIEW 而不是 PROCESS。</p>
     */
    PersonAlgorithmRerunRespDTO rerunAlgorithm(String eventId);

    /**
     * 查询算法任务最新状态（轮询接口）。
     *
     * <p>库内未进终态时向适配层取最新并落库；
     * 任务成功时回写事件的 algorithm_task_id / algorithm_status /
     * confidence / behavior_type，但<b>不动 process_status</b>。</p>
     */
    PersonAlgorithmTaskRespDTO getAlgorithmTask(String taskId);

    /** 查询某事件的全部算法任务，按开始时间倒序（详情弹窗兜底数据源） */
    List<PersonAlgorithmTaskRespDTO> listAlgorithmTasks(String eventId);
}
