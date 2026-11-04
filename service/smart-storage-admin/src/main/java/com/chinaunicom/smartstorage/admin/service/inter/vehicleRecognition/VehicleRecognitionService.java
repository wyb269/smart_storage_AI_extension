package com.chinaunicom.smartstorage.admin.service.inter.vehicleRecognition;

import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.TodayVehicleRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleAlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.vehicleRecognition.VehicleAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

/**
 * 车辆智能识别主业务服务。
 *
 * <p>Controller 只做参数校验与统一响应，本接口负责业务状态组合、
 * 识别记录查询、人工核实与并发控制。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
public interface VehicleRecognitionService {

    /** 查询今日识别车辆（左侧列表），必须按登录态做数据范围过滤 */
    List<TodayVehicleRespDTO> listTodayVehicle(int limit);

    /**
     * 获取指定识别点位摄像头的短时播放信息。
     *
     * <p>返回的 playableUrl 必须是短时签名地址，
     * 且不得包含摄像头账号、密码或永久 Token。</p>
     */
    ActionResult<StreamInfoRespDTO, String> getStreamInfo(String pointId);

    /** 查询首页最近识别记录，按识别时间倒序 */
    List<VehicleEventRespDTO> listRecentEvent(int limit);

    /**
     * 历史记录服务端分页查询。
     *
     * <p>分页由 {@code @QueryByPage} 切面完成，本方法只返回当前页数据。</p>
     */
    List<VehicleEventRespDTO> listEventByCondition(VehicleEventQueryReqDTO query);

    /** 查询识别记录详情、证据与核实记录 */
    VehicleEventDetailRespDTO getEventDetail(String eventId);

    /**
     * 人工核实识别记录。
     *
     * <p>使用记录版本号做乐观锁并发控制；核实人从登录态获取，
     * 不信任前端传入。必须已认证且记录在本用户数据范围内。
     * 产品口径与遮挡一致：页面走「查看即标记」，前端不调用本方法，
     * 保留 PROCESS 权限位以便后续开启误报处理链路。</p>
     */
    ActionResult<String, String> processEvent(String eventId, String verifyResult,
                                              String verifyOpinion, Integer eventVersion);

    /**
     * 查看识别记录后自动标记为已核实。
     *
     * <p>仅当当前为 PENDING 时写成 VERIFIED，重复调用无副作用。</p>
     *
     * <p>刻意做成独立接口而不是让 {@code getEventDetail} 顺带写库：
     * 读接口一旦带副作用，就会在轮询路径上产生意外写入。</p>
     */
    ActionResult<String, String> markEventViewed(String eventId);

    /** 按查询条件查询导出数据（不分页，受服务端条数上限约束） */
    List<VehicleEventRespDTO> listEventByConditionForExport(VehicleExportReqDTO exportReqDTO);

    /* ==================== 算法任务 / 重新识别（契约 §1） ==================== */

    /**
     * 对某识别记录发起「重新识别」：提交算法任务、落一行受理记录后立即返回。
     *
     * <p>算法不在请求里同步长等（第 24.1 节），由前端轮询 {@link #getAlgorithmTask(String)}。
     * 只刷新算法结论，不产生人工核实结论，写入任务一律 {@code is_simulated=TRUE}。</p>
     */
    VehicleAlgorithmRerunRespDTO rerunAlgorithm(String eventId);

    /** 查询算法任务最新进度（前端轮询入口）；未进终态时向网关取最新并落库 */
    VehicleAlgorithmTaskRespDTO getAlgorithmTask(String taskId);

    /** 列出某识别记录的全部算法任务（按开始时间倒序） */
    List<VehicleAlgorithmTaskRespDTO> listAlgorithmTasks(String eventId);
}
