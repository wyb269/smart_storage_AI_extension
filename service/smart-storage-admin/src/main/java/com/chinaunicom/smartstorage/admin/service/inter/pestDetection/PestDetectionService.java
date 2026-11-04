package com.chinaunicom.smartstorage.admin.service.inter.pestDetection;

import com.chinaunicom.smartstorage.admin.dto.pestDetection.MonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestAlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.PestExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.pestDetection.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

/**
 * 粮面虫害智能识别主业务服务。
 *
 * <p>Controller 只做参数校验与统一响应，本接口负责业务状态组合、
 * 检测记录查询、人工处理与并发控制。</p>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
public interface PestDetectionService {

    /** 查询当前用户可见的粮面点位状态，必须按登录态做数据范围过滤 */
    List<MonitorPointRespDTO> listMonitorPoint();

    /**
     * 获取指定粮面点位绑定摄像头的短时播放信息。
     *
     * <p>返回的 playableUrl 必须是短时签名地址，且不得包含摄像头账号、密码或永久 Token。</p>
     */
    ActionResult<StreamInfoRespDTO, String> getStreamInfo(String pointId);

    /** 查询首页最近检测记录，按采样时间倒序 */
    List<PestEventRespDTO> listRecentEvent(int limit);

    /**
     * 历史记录服务端分页查询。
     *
     * <p>分页由 {@code @QueryByPage} 切面完成，本方法只返回当前页数据。</p>
     */
    List<PestEventRespDTO> listEventByCondition(PestEventQueryReqDTO query);

    /** 查询检测记录详情、证据与处理记录 */
    PestEventDetailRespDTO getEventDetail(String eventId);

    /**
     * 人工核实检测记录。
     *
     * <p>使用事件版本号做乐观锁并发控制；处理人从登录态获取，不信任前端传入。
     * 必须已认证且记录在本用户数据范围内。</p>
     */
    ActionResult<String, String> processEvent(String eventId, String processResult,
                                              String processOpinion, Integer eventVersion);

    /**
     * 查看检测记录后自动标记为已处理。
     *
     * <p>产品口径与遮挡一致：前端无独立处理入口，点开详情即视为已处理。
     * 仅当当前为 PENDING 时写成 DONE，重复调用无副作用。</p>
     *
     * <p>刻意做成独立接口而不是让 {@code getEventDetail} 顺带写库：
     * 读接口一旦带副作用，就会在处理链路与轮询路径上产生意外写入。</p>
     */
    ActionResult<String, String> markEventViewed(String eventId);

    /** 按查询条件查询导出数据（不分页，受服务端条数上限约束） */
    List<PestEventRespDTO> listEventByConditionForExport(PestExportReqDTO exportReqDTO);

    /* ==================== 算法任务（「重新识别」） ==================== */

    /**
     * 对一条检测记录提交「重新识别」。
     *
     * <p>算法不得在页面请求里同步长等：校验事件存在且在数据范围内后
     * 组装统一算法输入 → 网关受理 → 落一行 QUEUED 任务（{@code is_simulated=TRUE}）
     * → 立即返回任务号，由前端轮询 {@link #getAlgorithmTask(String)} 取结果。</p>
     */
    PestAlgorithmRerunRespDTO rerunAlgorithm(String eventId);

    /**
     * 查询算法任务最新状态（轮询接口）。
     *
     * <p>先查库；未进终态时调网关取最新并落库（状态/结论/置信度/模型/版本/耗时/
     * 检测框/错误码）。任务成功时回写事件的 {@code algorithm_task_id}、
     * {@code algorithm_status}、{@code confidence} 与本功能结论列，
     * <b>不触碰 {@code process_status}</b>。</p>
     */
    PestAlgorithmTaskRespDTO getAlgorithmTask(String taskId);

    /** 查询某事件的历史算法任务，按 started_at 倒序，供详情弹窗任务表用 */
    List<PestAlgorithmTaskRespDTO> listAlgorithmTasks(String eventId);
}
