package com.chinaunicom.smartstorage.admin.service.inter.card;

import com.chinaunicom.smartstorage.admin.dto.card.SmartCardQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.card.SwipeCardReqDTO;
import com.chinaunicom.smartstorage.admin.dto.card.SwipeCardRespDTO;
import com.chinaunicom.smartstorage.admin.entity.card.SmartCard;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

/**
 * (SmartCard)表服务接口
 *
 * @author liuchink
 * @since 2026-01-22 13:35:41
 */
public interface SmartCardService {
    ActionResult<String, String> saveSmartCard(SmartCard smartCard);

    ActionResult<String, String> updateSmartCard(SmartCard smartCard);

    ActionResult<String, String> deleteSmartCard(List<SmartCard> listSmartCard);

    List<SmartCard> listSmartCardByCondition(SmartCardQueryReqDTO smartCardQueryReqDTO);

    SmartCard getSmartCardById(String id);

    ActionResult<SwipeCardRespDTO, String> inSwipeCard(SwipeCardReqDTO swipeCardReqDTO);

    ActionResult<SwipeCardRespDTO, String> outSwipeCard(SwipeCardReqDTO swipeCardReqDTO);
}
