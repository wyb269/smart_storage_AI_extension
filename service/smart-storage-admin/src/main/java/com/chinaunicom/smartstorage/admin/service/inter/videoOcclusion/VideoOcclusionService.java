package com.chinaunicom.smartstorage.admin.service.inter.videoOcclusion;

import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.CameraStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.OcclusionExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm.OcclusionAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.dto.videoOcclusion.algorithm.RerunAlgorithmRespDTO;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

/**
 * 视频遮挡检测主业务服务。
 *
 * <p>Controller 只做参数校验与统一响应，本接口负责业务状态组合、
 * 事件查询、人工处理与并发控制。</p>
 *
 * @author aiVision 功能族 · 视频遮挡检测
 */
public interface VideoOcclusionService {

    /** 查询当前用户可见的摄像头状态，必须按登录态做数据范围过滤 */
    List<CameraStatusRespDTO> listCameraStatus();

    /**
     * 获取指定摄像头的短时播放信息。
     *
     * <p>返回的 playableUrl 必须是短时签名地址，
     * 且不得包含摄像头账号、密码或永久 Token。</p>
     */
    ActionResult<StreamInfoRespDTO, String> getStreamInfo(String cameraId);

    /** 查询首页最近事件，按发生时间倒序 */
    List<OcclusionEventRespDTO> listRecentEvent(int limit);

    /**
     * 历史记录服务端分页查询。
     *
     * <p>分页由 {@code @QueryByPage} 切面完成，本方法只返回当前页数据。</p>
     */
    List<OcclusionEventRespDTO> listEventByCondition(OcclusionEventQueryReqDTO query);

    /** 查询事件详情、证据与处理记录 */
    OcclusionEventDetailRespDTO getEventDetail(String eventId);

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
     * <p>产品口径与仓门异动一致：页面只保留未处理/已处理两态，
     * 点开详情即视为已处理。仅当当前为 PENDING 时写入，重复调用无副作用。</p>
     *
     * <p>刻意做成独立接口而不是让 {@code getEventDetail} 顺带写库：
     * 读接口一旦带副作用，就会在处理链路与轮询路径上产生意外写入。</p>
     */
    ActionResult<String, String> markEventViewed(String eventId);

    /** 按查询条件查询导出数据（不分页，受服务端条数上限约束） */
    List<OcclusionEventRespDTO> listEventByConditionForExport(OcclusionExportReqDTO exportReqDTO);

    /* ==================== 算法任务（契约 §1「重新识别」） ==================== */

    /**
     * 对既有事件发起一次算法重新识别。
     *
     * <p>提交 → 立即返回 taskId → 前端轮询 {@link #getAlgorithmTask}。
     * 算法不得在页面请求里同步长等（AI_HANDOFF 第 24.1 节）。
     * 必须已认证且事件在本用户数据范围内。</p>
     */
    RerunAlgorithmRespDTO rerunAlgorithm(String eventId);

    /**
     * 查询单个算法任务；未进终态时向网关取最新结果并落库。
     *
     * <p>任务成功时回写事件的 algorithm_task_id / algorithm_status /
     * algorithm_result / confidence，但不动 process_status。</p>
     */
    OcclusionAlgorithmTaskRespDTO getAlgorithmTask(String taskNo);

    /** 查询某事件的算法任务列表（started_at 倒序，最多 20 条），供详情弹窗任务表 */
    List<OcclusionAlgorithmTaskRespDTO> listAlgorithmTasks(String eventId);
}
