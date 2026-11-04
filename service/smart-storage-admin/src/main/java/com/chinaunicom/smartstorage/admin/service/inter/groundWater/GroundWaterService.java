package com.chinaunicom.smartstorage.admin.service.inter.groundWater;

import com.chinaunicom.smartstorage.admin.dto.groundWater.MonitorPointStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.WaterExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.algorithm.RerunAlgorithmRespDTO;
import com.chinaunicom.smartstorage.admin.dto.groundWater.algorithm.WaterAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

/**
 * 地面积水监测主业务服务。
 *
 * <p>Controller 只做参数校验与统一响应，本接口负责业务状态组合、
 * 事件查询、人工处理与并发控制。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
public interface GroundWaterService {

    /** 查询当前用户可见的监测点位状态，必须按登录态做数据范围过滤 */
    List<MonitorPointStatusRespDTO> listMonitorPoint();

    /**
     * 获取指定监测点位绑定摄像头的短时播放信息。
     *
     * <p>返回的 playableUrl 必须是短时签名地址，
     * 且不得包含摄像头账号、密码或永久 Token。</p>
     */
    ActionResult<StreamInfoRespDTO, String> getStreamInfo(String pointId);

    /** 查询首页最近事件，按发生时间倒序 */
    List<WaterEventRespDTO> listRecentEvent(int limit);

    /**
     * 历史记录服务端分页查询。
     *
     * <p>分页由 {@code @QueryByPage} 切面完成，本方法只返回当前页数据。</p>
     */
    List<WaterEventRespDTO> listEventByCondition(WaterEventQueryReqDTO query);

    /** 查询事件详情、证据与处理记录 */
    WaterEventDetailRespDTO getEventDetail(String eventId);

    /**
     * 人工核实事件。
     *
     * <p>使用事件版本号做乐观锁并发控制；处理人从登录态获取，
     * 不信任前端传入。必须已认证且事件在本用户数据范围内。</p>
     *
     * <p><b>本期前端不调用</b>（产品口径为「查看即标记」），
     * 但保留实现与 PROCESS 权限位，便于后续开启真实处理链路。</p>
     */
    ActionResult<String, String> processEvent(String eventId, String processResult,
                                              String processOpinion, Integer eventVersion);

    /**
     * 查看事件后自动标记为已处理。
     *
     * <p>三态模型下只把 {@code PENDING}（未核实）写成 {@code DONE}；
     * {@code PROCESSING}（处理中）说明已有人在跟进，不覆盖。
     * 仅当当前为 PENDING 时写入，重复调用无副作用。</p>
     *
     * <p>刻意做成独立接口而不是让 {@code getEventDetail} 顺带写库：
     * 读接口一旦带副作用，就会在处理链路与轮询路径上产生意外写入。</p>
     */
    ActionResult<String, String> markEventViewed(String eventId);

    /** 按查询条件查询导出数据（不分页，受服务端条数上限约束） */
    List<WaterEventRespDTO> listEventByConditionForExport(WaterExportReqDTO exportReqDTO);

    /* ==================== 算法任务（「重新识别」链路，契约 §1） ==================== */

    /**
     * 对指定事件重新提交算法识别。
     *
     * <p>流程严格遵循「不许同步长等」：校验事件存在且在数据范围内 →
     * 组装统一算法输入 → 网关受理 → 落一行 QUEUED 任务 → 立即返回任务编号。
     * 进度由前端轮询 {@link #getAlgorithmTask(String)} 获取。</p>
     *
     * <p>权限口径：VIEW。重跑本质是重新取证，只产算法侧数据，
     * 不产生人工结论、不触碰 process_status，因此不要求 PROCESS。</p>
     */
    RerunAlgorithmRespDTO rerunAlgorithm(String eventId);

    /**
     * 查询算法任务最新状态。
     *
     * <p>先查库；未进终态时向网关取最新产出并落库
     * （状态 / 结论 / 置信度 / 模型与版本 / 耗时 / 检测框 / 错误码）。
     * 任务成功时把结论回写事件的 algorithm_* / confidence / water_type，
     * 但<b>不动 process_status</b>。</p>
     */
    WaterAlgorithmTaskRespDTO getAlgorithmTask(String taskId);

    /** 查询某事件的全部算法任务，按 started_at 倒序（详情弹窗算法任务表数据源） */
    List<WaterAlgorithmTaskRespDTO> listAlgorithmTasks(String eventId);
}
