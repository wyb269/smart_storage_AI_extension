package com.chinaunicom.smartstorage.admin.service.impl.aggregation;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.chinaunicom.smartstorage.admin.component.MyWordDenyComponent;
import com.chinaunicom.smartstorage.admin.dto.aggregation.sensitive.SensitiveWordsQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.SensitiveWords;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.SensitiveWordsMapper;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.SensitiveWordsService;
import com.chinaunicom.smartstorage.common.enumbean.OptEnum;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author liuchink
 * @since 2024/9/30 14:13
 */
@Service
public class SensitiveWordsServiceImpl implements SensitiveWordsService {
    @Resource
    private SensitiveWordsMapper sensitiveWordsMapper;
    @Resource
    private SensitiveWordBs sensitiveWordBs;
    @Resource
    private MyWordDenyComponent myWordDenyComponent;
    @Resource
    private ActionResultService actionResultService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveSensitiveWords(SensitiveWords sensitiveWords) {
        ActionResult<String, String> actionResult = check(sensitiveWords, OptEnum.ADD);
        if (!actionResult.getStatus().isSuccess()) {
            return actionResult;
        }
        sensitiveWordsMapper.insert(sensitiveWords);
        return actionResultService.callBackResult();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateSensitiveWords(SensitiveWords sensitiveWords) {
        ActionResult<String, String> actionResult = check(sensitiveWords, OptEnum.UPDATE);
        if (!actionResult.getStatus().isSuccess()) {
            return actionResult;
        }
        sensitiveWordsMapper.updateById(sensitiveWords);
        return actionResultService.callBackResult();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteSensitiveWords(List<SensitiveWords> listSensitiveWords) {
        sensitiveWordsMapper.deleteByIds(listSensitiveWords);
        return actionResultService.callBackResult();
    }

    @Override
    public List<SensitiveWords>
        listSensitiveWordsByConditionOnPage(SensitiveWordsQueryReqDTO sensitiveWordsQueryReqDTO) {
        return sensitiveWordsMapper.selectList(Wrappers.lambdaQuery(SensitiveWords.class).like(
            CharSequenceUtil.isNotBlank(sensitiveWordsQueryReqDTO.getSenWord()), SensitiveWords::getSenWord,
            sensitiveWordsQueryReqDTO.getSenWord()));
    }

    @Override
    public SensitiveWords getSensitiveWordsById(String id) {
        return sensitiveWordsMapper.selectById(id);
    }

    private ActionResult<String, String> check(SensitiveWords sensitiveWords, OptEnum optEnum) {
        final SensitiveWords sensitiveWordsSame = sensitiveWordsMapper.selectOne(
            Wrappers.lambdaQuery(SensitiveWords.class).eq(SensitiveWords::getSenWord, sensitiveWords.getSenWord()));
        if (sensitiveWordsSame != null && !sensitiveWordsSame.getId().equals(sensitiveWords.getId())) {
            return actionResultService.callBackResultFail("敏感词已存在");
        }
        return actionResultService.callBackResult();
    }
}
