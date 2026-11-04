package com.chinaunicom.smartstorage.admin.service.inter.perimeterIntrusion.algorithm;

import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.algorithm.PerimeterAlgorithmTaskInputDTO;
import com.chinaunicom.smartstorage.admin.dto.perimeterIntrusion.algorithm.PerimeterAlgorithmTaskOutputDTO;

/**
 * 周界入侵算法适配层网关。
 *
 * <p>依据 AI_HANDOFF.md 第 15.3、23、24 节：</p>
 * <ul>
 *   <li>页面和业务后端<b>不得直接依赖</b>真实算法包的语言、函数、文件结构或原始输出。</li>
 *   <li>真实算法必须位于本适配层之后。</li>
 *   <li>真实算法包未到之前，只实现模拟适配器，不得猜测并写死真实算法依赖。</li>
 *   <li>真实算法到达后<b>只需新增实现类</b>，不得破坏页面接口和业务事件模型（第 24.5 节）。</li>
 * </ul>
 *
 * <p><b>主体维度</b>：本功能的主体是<b>防区段</b>，以 {@code pointId}
 * （指向 {@code ai_perimeter_monitor_point.id}）为准，
 * 摄像头只是防区段绑定的属性，因此输入里同时保留 {@code cameraConfigId}、
 * {@code cameraId}、{@code cameraCode}。</p>
 *
 * <p><b>当前实现</b>：仅 {@code MockPerimeterIntrusionAlgorithmGateway}。
 * 真实算法接入方式（本地进程/容器/gRPC/HTTP）必须在完成算法包准入检查后决定（第 36 节）。</p>
 *
 * <p><b>本期形态</b>：预留接缝，<b>不接入任何调用方</b>，无任何注入点。
 * 与遮挡那一套的交付现状保持一致。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
public interface PerimeterIntrusionAlgorithmGateway {

    /**
     * 提交算法任务。
     *
     * <p>算法<b>不得</b>由页面请求同步长时间等待（第 24.1 节）。
     * 实现应快速返回任务已受理，由查询接口获取进度。</p>
     *
     * @param input 统一算法输入
     * @return 任务受理结果，含任务编号与初始状态
     */
    String submitTask(PerimeterAlgorithmTaskInputDTO input);

    /**
     * 查询算法任务状态。
     *
     * @param taskId 算法任务编号
     * @return 统一算法输出；任务尚未完成时 executionStatus 为 QUEUED 或 RUNNING
     */
    PerimeterAlgorithmTaskOutputDTO queryTask(String taskId);

    /**
     * 将真实算法原始输出转换为统一结果。
     *
     * <p>真实算法接入时由新增的适配器实现此方法，
     * 把厂商格式转换为 {@link PerimeterAlgorithmTaskOutputDTO}。</p>
     *
     * @param rawOutput 原始输出（JSON 字符串等）
     * @return 统一算法输出
     */
    PerimeterAlgorithmTaskOutputDTO toStandardResult(String rawOutput);

    /**
     * 适配器名称，用于版本追溯（第 25.2 节）。
     *
     * @return 适配器标识
     */
    String adapterName();
}
