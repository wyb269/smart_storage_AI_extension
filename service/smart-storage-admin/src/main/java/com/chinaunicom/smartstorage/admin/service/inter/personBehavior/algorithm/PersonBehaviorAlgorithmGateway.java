package com.chinaunicom.smartstorage.admin.service.inter.personBehavior.algorithm;

import com.chinaunicom.smartstorage.admin.dto.personBehavior.algorithm.PersonAlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.personBehavior.algorithm.PersonAlgorithmTaskOutputDTO;

/**
 * 人员行为算法适配层网关。
 *
 * <ul>
 *   <li>页面和业务后端<b>不得直接依赖</b>真实算法包的语言、函数、文件结构或原始输出。</li>
 *   <li>真实算法必须位于本适配层之后。</li>
 *   <li>真实算法包未到之前，只实现模拟适配器，不得猜测并写死真实算法依赖。</li>
 *   <li>真实算法到达后<b>只需新增实现类</b>，不得破坏页面接口和业务事件模型。</li>
 * </ul>
 *
 * <p><b>主体维度</b>：本功能的监测对象是「区域」而不是摄像头，
 * 主体是 {@code pointId}（指向 {@code ai_person_monitor_point.id}），
 * 并保留区域内绑定摄像头的 {@code cameraId}、{@code cameraCode} 两个标识。</p>
 *
 * <p><b>当前实现</b>：仅 {@code MockPersonBehaviorAlgorithmGateway}。
 * 真实算法接入方式（本地进程/容器/gRPC/HTTP）必须在完成算法包准入检查后决定。</p>
 *
 * <p><b>本期形态</b>：预留接缝，<b>不接入任何调用方</b>，无任何注入点。
 * 与仓门异动、视频遮挡两侧的交付现状保持一致。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
public interface PersonBehaviorAlgorithmGateway {

    /**
     * 提交算法任务。
     *
     * <p>算法<b>不得</b>由页面请求同步长时间等待。
     * 实现应快速返回任务已受理，由查询接口获取进度。</p>
     *
     * @param input 统一算法输入
     * @return 任务受理结果，含任务编号与初始状态
     */
    String submitTask(PersonAlgorithmTaskInputDTO input);

    /**
     * 查询算法任务状态。
     *
     * @param taskId 算法任务编号
     * @return 统一算法输出；任务尚未完成时 executionStatus 为 QUEUED 或 RUNNING
     */
    PersonAlgorithmTaskOutputDTO queryTask(String taskId);

    /**
     * 将真实算法原始输出转换为统一结果。
     *
     * <p>真实算法接入时由新增的适配器实现此方法，
     * 把厂商格式转换为 {@link PersonAlgorithmTaskOutputDTO}。</p>
     *
     * @param rawOutput 原始输出（JSON 字符串等）
     * @return 统一算法输出
     */
    PersonAlgorithmTaskOutputDTO toStandardResult(String rawOutput);

    /**
     * 适配器名称，用于版本追溯。
     *
     * @return 适配器标识
     */
    String adapterName();
}
