package com.chinaunicom.smartstorage.admin.mapper.aivision.personBehavior;

import com.chinaunicom.smartstorage.admin.entity.personBehavior.AiPersonAlgorithmTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 人员行为算法任务表（{@code ai_person_algorithm_task}）数据访问。
 *
 * <p>与 {@link PersonBehaviorMapper} 分开：本表由「重新识别」链路读写，
 * 列形态与事件表无关，混在一个 Mapper 里会稀释两边的范围条件注释。</p>
 *
 * <p><b>数据范围要求</b>：两个查询方法都必须带仓库范围三态下推
 * （非全域且有授权仓库 → IN；非全域且无授权仓库 → 恒假；全域 → 不追加），
 * 与 PersonBehaviorMapper.xml 的规则一致。</p>
 *
 * <p>单参数方法也必须写 {@code @Param}（仓门那一版的教训）。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
public interface PersonAlgorithmTaskMapper {

    /** 插入一行算法任务（39 列显式列出，detectedCount 不是表列） */
    int insertTask(AiPersonAlgorithmTask task);

    /** 按任务编号更新执行结果字段（状态/结论/置信度/模型/版本/耗时/检测框/错误码等） */
    int updateTaskResult(AiPersonAlgorithmTask task);

    /**
     * 按任务编号查询（带数据范围）。
     *
     * @return 越权或不存在时返回 null
     */
    AiPersonAlgorithmTask selectByTaskNo(@Param("taskNo") String taskNo,
                                         @Param("unrestricted") boolean unrestricted,
                                         @Param("warehouseIds") List<Long> warehouseIds);

    /**
     * 查询某事件的全部算法任务，按 COALESCE(started_at, created_at) 倒序。
     */
    List<AiPersonAlgorithmTask> selectByEventId(@Param("eventId") Long eventId,
                                                @Param("unrestricted") boolean unrestricted,
                                                @Param("warehouseIds") List<Long> warehouseIds,
                                                @Param("limit") int limit);
}
