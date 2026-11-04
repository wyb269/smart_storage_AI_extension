package com.chinaunicom.smartstorage.admin.service.inter.beltMachine;

import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineMonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineStreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.algorithm.BeltMachineAlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.algorithm.BeltMachineAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

/**
 * 皮带机下站人监测主业务服务。
 *
 * <p>Controller 只做参数校验与统一响应，本接口负责业务状态组合、
 * 事件查询、人工处理与并发控制。</p>
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
public interface BeltMachineService {

    /** 查询当前用户可见的皮带机监测点位状态，必须按登录态做数据范围过滤 */
    List<BeltMachineMonitorPointRespDTO> listMonitorPoint();

    /**
     * 获取指定皮带机点位绑定摄像头的短时播放信息。
     *
     * <p>返回的 playableUrl 必须是短时签名地址，
     * 且不得包含摄像头账号、密码或永久 Token。</p>
     */
    ActionResult<BeltMachineStreamInfoRespDTO, String> getStreamInfo(String pointId);

    /** 查询首页最近事件，按发生时间倒序 */
    List<BeltMachineEventRespDTO> listRecentEvent(int limit);

    /**
     * 历史记录服务端分页查询。
     *
     * <p>分页由 {@code @QueryByPage} 切面完成，本方法只返回当前页数据。</p>
     */
    List<BeltMachineEventRespDTO> listEventByCondition(BeltMachineEventQueryReqDTO query);

    /** 查询事件详情、证据与处理记录 */
    BeltMachineEventDetailRespDTO getEventDetail(String eventId);

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
     * <p>产品口径与仓门异动、视频遮挡一致：点开详情即视为已处理。
     * 仅当当前为 PENDING（未核实）时写入，重复调用无副作用。</p>
     *
     * <p>刻意做成独立接口而不是让 {@code getEventDetail} 顺带写库：
     * 读接口一旦带副作用，就会在处理链路与轮询路径上产生意外写入。</p>
     */
    ActionResult<String, String> markEventViewed(String eventId);

    /** 按查询条件查询导出数据（不分页，受服务端条数上限约束） */
    List<BeltMachineEventRespDTO> listEventByConditionForExport(BeltMachineExportReqDTO exportReqDTO);

    /* ==================== 算法任务（重新识别链路） ==================== */

    /**
     * 对指定事件发起「重新识别」。
     *
     * <p>依据 AI_HANDOFF 第 24.1 节不在请求里同步等算法：
     * 校验事件存在、在数据范围内且点位已绑定摄像头 →
     * 组装统一算法输入 → 网关受理 → 落一行 QUEUED/RUNNING 任务 → 立即返回任务标识。
     * 进度由前端轮询 {@link #getAlgorithmTask}。</p>
     *
     * <p>当前唯一实现是 Mock 适配器，返回的任务恒为模拟结果（is_simulated=TRUE）。</p>
     */
    BeltMachineAlgorithmRerunRespDTO rerunAlgorithm(String eventId);

    /**
     * 查询算法任务最新状态。
     *
     * <p>先查库（带数据范围）；未进终态时取网关最新结果落库；
     * 任务成功时回写事件的算法列与结论类型，但不动人工处理状态。</p>
     *
     * @return 完整任务视图；不存在或越权时抛业务异常
     */
    BeltMachineAlgorithmTaskRespDTO getAlgorithmTask(String taskNo);

    /** 查询事件的算法任务列表（开始时间倒序，最多 20 条），供详情弹窗展示 */
    List<BeltMachineAlgorithmTaskRespDTO> listAlgorithmTasks(String eventId);
}
