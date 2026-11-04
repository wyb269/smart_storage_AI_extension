package com.chinaunicom.smartstorage.admin.service.inter.aggregation;

import java.util.List;

import com.chinaunicom.smartstorage.admin.dto.aggregation.sensitive.SensitiveWordsQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.SensitiveWords;
import com.chinaunicom.smartstorage.common.result.ActionResult;

/**
 * @author liuchink
 * @since 2024/9/30 14:03
 */
public interface SensitiveWordsService {

    ActionResult<String, String> saveSensitiveWords(SensitiveWords sensitiveWords);

    ActionResult<String, String> updateSensitiveWords(SensitiveWords sensitiveWords);

    ActionResult<String, String> deleteSensitiveWords(List<SensitiveWords> listSensitiveWords);

    List<SensitiveWords> listSensitiveWordsByConditionOnPage(SensitiveWordsQueryReqDTO sensitiveWordsQueryReqDTO);

    SensitiveWords getSensitiveWordsById(String id);
}
