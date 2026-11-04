package com.chinaunicom.smartstorage.admin.service.inter.doorAnomaly;

import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.AnomalyEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.DoorAnomalyExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.DoorStatusRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventProcessReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.StreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.algorithm.AlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.algorithm.AlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

/**
 * 仓门异动主业务服务。
 *
 * <p>职责（依据 AI_HANDOFF.md 第 23 节）：业务状态组合、事件查询、
 * 人工处理和并发控制。Controller 只做参数校验与统一响应。</p>
 *
 * <p><b>禁止事项</b>：</p>
 * <ul>
 *   <li>禁止在 Controller 中直接运行模型。</li>
 *   <li>禁止用删除或覆盖算法结果的方式完成事件处理。</li>
 *   <li>禁止让算法结论直接改成人工处理结论。</li>
 * </ul>
 *
 * @author 仓门异动功能
 */
public interface DoorAnomalyService {

    /**
     * 查询当前用户可见的仓门状态。
     *
     * <p>必须按登录用户的行政区与组织机构做数据范围过滤。</p>
     *
     * @return 仓门状态列表
     */
    List<DoorStatusRespDTO> listDoorStatus();

    /**
     * 获取指定仓门的短时播放信息。
     *
     * <p>返回的 playableUrl 必须是短时签名地址，
     * 且不得包含摄像头账号、密码或永久 Token。</p>
     *
     * @param doorId 仓门标识
     * @return 播放信息；不可播放时 canPlay 为 false 并给出原因
     */
    ActionResult<StreamInfoRespDTO, String> getStreamInfo(String doorId);

    /**
     * 查询首页最近事件。
     *
     * @param limit 展示条数，首页建议 5
     * @return 按发生时间倒序的事件列表
     */
    List<AnomalyEventRespDTO> listRecentEvent(int limit);

    /**
     * 历史记录服务端分页查询。
     *
     * <p>分页由 {@code @QueryByPage} 切面完成，
     * 本方法只返回当前页数据，不要自行分页。</p>
     *
     * @param eventQueryReqDTO 查询条件
     * @return 当前页事件列表
     */
    List<AnomalyEventRespDTO> listEventByCondition(EventQueryReqDTO eventQueryReqDTO);

    /**
     * 查询事件详情、证据与处理记录。
     *
     * @param eventId 事件标识
     * @return 事件详情
     */
    EventDetailRespDTO getEventDetail(String eventId);

    /**
     * 人工核实事件。
     *
     * <p>使用事件版本号做乐观锁并发控制。版本已变化时
     * 必须拒绝覆盖并提示「事件已被其他用户处理」。
     * 处理人与处理时间从登录态获取，不信任前端传入。</p>
     *
     * <p><b>安全约束</b>：必须已认证（无法解析登录态时直接拒绝），
     * 且必须校验事件所属组织/区域在本用户数据范围内。</p>
     *
     * @param eventId           事件标识
     * @param eventProcessReqDTO 处理请求
     * @return 处理结果提示
     */
    ActionResult<String, String> processEvent(String eventId, EventProcessReqDTO eventProcessReqDTO);

    /**
     * 查看事件后自动标记为已处理。
     *
     * <p><b>产品语义（2026-10-21 变更）**：本功能已取消人工处理入口，
     * 处理状态只保留「未处理 / 已处理」两种。用户点开事件详情即视为已处理。</p>
     *
     * <p><b>为什么是独立接口而不是让 {@code getEventDetail} 顺带写库</b>：
     * {@code processEvent} 内部会先调 {@code getEventDetail} 做数据范围与
     * 终态校验，读接口一旦带副作用就会在处理链路上产生意外写入，
     * 且详情接口会被列表刷新、轮询等路径反复调用。</p>
     *
     * <p><b>幂等</b>：仅当事件当前为 {@code PENDING} 时才写入；
     * 已是其它状态时直接返回成功且不产生任何写入，避免每次查看都追加一条审计。</p>
     *
     * <p><b>审计</b>：写入时仍追加一条处理记录，但处理意见固定标注为
     * 「查看事件详情后自动标记为已处理」，以便与真实人工核实结论区分。</p>
     *
     * @param eventId 事件标识
     * @return 标记结果提示
     */
    ActionResult<String, String> markEventViewed(String eventId);

    /**
     * 按查询条件查询导出数据。
     *
     * <p>与 {@link #listEventByCondition(EventQueryReqDTO)} 的区别：
     * 此方法不分页，受服务端条数上限约束（第 18.5 节约 5000 条）。
     * 导出字段不含图片证据，也不含短时媒体地址（第 29.5 节）。</p>
     *
     * @param exportReqDTO 导出条件
     * @return 待导出的事件列表
     */
    List<AnomalyEventRespDTO> listEventByConditionForExport(DoorAnomalyExportReqDTO exportReqDTO);

    /* ==================== 算法任务 / 重新识别（契约 AI_VISION_ALGORITHM_DATA_INTERACTION.md §1） ==================== */

    /**
     * 对某条事件发起「重新识别」。
     *
     * <p>校验事件存在且在数据范围内 → 组装统一算法输入 →
     * {@code gateway.submitTask()} → 落一行 QUEUED 任务（{@code is_simulated=TRUE}）
     * → 立即返回受理回执。<b>不在请求里同步等待算法完成</b>（第 24.1 节），
     * 由前端轮询 {@link #getAlgorithmTask(String)} 获取进度。</p>
     *
     * @param eventId 事件标识
     * @return 受理回执（taskId / QUEUED / simulated）
     */
    AlgorithmRerunRespDTO rerunAlgorithm(String eventId);

    /**
     * 查询算法任务最新进度。
     *
     * <p>先查库；未进终态时调用 {@code gateway.queryTask(taskId)} 取最新 →
     * 落库（更新状态/结论/置信度/模型/版本/耗时/检测框/错误码）→
     * 若成功则回写事件的 {@code algorithm_task_id / algorithm_status / confidence}
     * 与本功能结论列，<b>不动 process_status</b>。</p>
     *
     * @param taskId 算法任务编号（即 taskNo）
     * @return 完整任务视图
     */
    AlgorithmTaskRespDTO getAlgorithmTask(String taskId);

    /**
     * 列出某事件的全部算法任务，按 started_at 倒序。
     *
     * @param eventId 事件标识
     * @return 任务数组
     */
    List<AlgorithmTaskRespDTO> listAlgorithmTasks(String eventId);
}
