package com.chinaunicom.smartstorage.admin.component;

import java.util.List;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.houbb.sensitive.word.api.IWordDeny;
import com.google.common.collect.Lists;
import com.chinaunicom.smartstorage.admin.entity.aggregation.SensitiveWords;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.SensitiveWordsMapper;

import jakarta.annotation.Resource;

/**
 * @author liuchink
 * @since 2024/9/30 14:40
 */
@Component
public class MyWordDenyComponent implements IWordDeny {
    @Resource
    private SensitiveWordsMapper sensitiveWordsMapper;

    @Override
    public List<String> deny() {
        List<String> list = Lists.newArrayList();
        final List<SensitiveWords> listSensitiveWords = sensitiveWordsMapper.selectList(Wrappers.lambdaQuery());
        if (!listSensitiveWords.isEmpty()) {
            for (SensitiveWords sensitiveWord : listSensitiveWords) {
                list.add(sensitiveWord.getSenWord());
            }
        }
        return list;
    }
}
