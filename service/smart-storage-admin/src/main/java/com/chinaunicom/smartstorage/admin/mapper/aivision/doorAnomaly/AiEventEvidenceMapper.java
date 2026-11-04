package com.chinaunicom.smartstorage.admin.mapper.aivision.doorAnomaly;

import com.chinaunicom.smartstorage.admin.dto.doorAnomaly.EventEvidenceRespDTO;
import com.chinaunicom.smartstorage.admin.entity.doorAnomaly.AiEventEvidence;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 事件证据 Mapper。
 *
 * <p>依据 AI_HANDOFF.md 第 17.4、27.5 节。</p>
 *
 * @author 仓门异动功能
 */
public interface AiEventEvidenceMapper extends MPJBaseMapper<AiEventEvidence> {

    /**
     * 查询事件的全部证据，按展示顺序排列。
     *
     * <p>返回对象仅含对象存储键，<b>不返回任何完整地址</b>；
     * 短时地址由 Service 层经 MediaGateway 签发（第 26.2 节）。</p>
     *
     * @param eventId 事件标识
     */
    List<EventEvidenceRespDTO> listEvidenceByEventId(@Param("eventId") Long eventId);
}