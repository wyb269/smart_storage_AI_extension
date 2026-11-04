package com.chinaunicom.smartstorage.admin.mapper.card;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinaunicom.smartstorage.admin.dto.card.SmartCardQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.card.SwipeCardReqDTO;
import com.chinaunicom.smartstorage.admin.dto.card.SwipeCardRespDTO;
import com.chinaunicom.smartstorage.admin.entity.card.SmartCard;

import java.util.List;

/**
 * (SmartCard)表数据库访问层
 *
 * @author liuchink
 * @since 2026-01-22 13:35:41
 */
public interface SmartCardMapper extends BaseMapper<SmartCard> {
    List<SmartCard> listSmartCardByCondition(SmartCardQueryReqDTO smartCardQueryReqDTO);

    SwipeCardRespDTO swipeCard(SwipeCardReqDTO swipeCardReqDTO);
}
