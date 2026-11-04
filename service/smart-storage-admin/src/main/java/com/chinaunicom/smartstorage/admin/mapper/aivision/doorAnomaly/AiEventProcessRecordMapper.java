package com.chinaunicom.smartstorage.admin.mapper.aivision.doorAnomaly;

import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.ProcessRecordRespDTO;
import com.chinaunicom.smartstorage.admin.entity.doorAnomaly.AiEventProcessRecord;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 事件处理记录 Mapper。
 *
 * <p>依据 AI_HANDOFF.md 第 17.6、27.6、29.4 节。</p>
 *
 * @author 仓门异动功能
 */
public interface AiEventProcessRecordMapper extends MPJBaseMapper<AiEventProcessRecord> {

    /**
     * 查询事件的全部处理记录，按时间正序。
     *
     * <p>追加式记录，返回<b>完整历史</b>，不得只取最后一次（第 17.6 节）。</p>
     *
     * @param eventId 事件标识
     */
    List<ProcessRecordRespDTO> listProcessRecords(@Param("eventId") Long eventId);

    /**
     * 追加写入一条处理记录。
     *
     * <p>即使主表更新成功也必须写入，用于审计追溯（第 29.4 节）。</p>
     *
     * @param id          主键（雪花 ID）
     * @param eventId     事件标识
     * @param eventNo     冗余事件编号
     * @param statusBefore 处理前状态
     * @param statusAfter  处理后状态
     * @param processResult 处理结果
     * @param processOpinion 处理意见
     * @param processedBy 处理人
     * @param processedAt 处理时间
     * @param source      操作来源
     * @param createdAt   创建时间
     */
    int appendProcessRecord(@Param("id") Long id,
                            @Param("eventId") Long eventId,
                            @Param("eventNo") String eventNo,
                            @Param("statusBefore") String statusBefore,
                            @Param("statusAfter") String statusAfter,
                            @Param("processResult") String processResult,
                            @Param("processOpinion") String processOpinion,
                            @Param("processedBy") String processedBy,
                            @Param("processedAt") LocalDateTime processedAt,
                            @Param("source") String source,
                            @Param("createdAt") LocalDateTime createdAt);
}