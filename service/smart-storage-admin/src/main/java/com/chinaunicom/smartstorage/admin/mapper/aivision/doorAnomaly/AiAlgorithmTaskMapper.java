package com.chinaunicom.smartstorage.admin.mapper.aivision.doorAnomaly;

import com.chinaunicom.smartstorage.admin.entity.doorAnomaly.AiAlgorithmTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 算法任务数据访问。
 *
 * <p>目标表 {@code ai_algorithm_task}（01 建表脚本 T4）。本 Mapper 仅在
 * {@code ai-vision.datasource.enabled=true} 时由 {@code AiVisionMybatisConfiguration}
 * 扫描注册；内存兜底模式走 {@code InMemoryDoorAnomalyRepository}，不依赖本接口。</p>
 *
 * <p><b>P0-6 数据范围</b>：读方法与写方法都按仓库集合下推过滤，
 * 与 {@code AiAnomalyEventMapper} 同一套语义——非全域且仓库集合为空时恒假。</p>
 *
 * @author 仓门异动功能
 */
public interface AiAlgorithmTaskMapper {

    /**
     * 落一行算法任务（受理时先写 QUEUED/RUNNING）。
     *
     * @return 受影响行数
     */
    int insertTask(AiAlgorithmTask task);

    /**
     * 按算法任务编号在数据范围内查任务（供 algorithmTask/{taskId} 轮询）。
     *
     * @param unrestricted 是否全域
     * @param warehouseIds 授权仓库集合
     * @return 越权或不存在返回 null
     */
    AiAlgorithmTask selectByTaskNoInScope(@Param("taskNo") String taskNo,
                                          @Param("unrestricted") boolean unrestricted,
                                          @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 按事件（candidate_event_id）列任务，started_at 倒序。
     *
     * <p>给详情弹窗的「算法任务」表与 listAlgorithmTasks 用。</p>
     */
    List<AiAlgorithmTask> selectByEventIdInScope(@Param("eventId") Long eventId,
                                                 @Param("unrestricted") boolean unrestricted,
                                                 @Param("warehouseIds") List<Long> warehouseIds,
                                                 @Param("limit") int limit);

    /**
     * 更新任务结果列（轮询拿到算法最新进度后落库）。
     *
     * <p>只更新执行/结论相关列，不改动受理时写入的主体字段。</p>
     *
     * @return 受影响行数
     */
    int updateTaskResult(AiAlgorithmTask task);
}
