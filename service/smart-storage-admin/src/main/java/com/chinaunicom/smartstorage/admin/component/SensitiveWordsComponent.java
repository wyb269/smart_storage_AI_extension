package com.chinaunicom.smartstorage.admin.component;

import org.springframework.stereotype.Component;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.chinaunicom.smartstorage.common.result.ActionResultService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * @author liuchink
 * @since 2024/10/15 16:47
 */
@Slf4j
@Component
public class SensitiveWordsComponent {
    @Resource
    private SensitiveWordBs sensitiveWordBs;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private MyWordDenyComponent myWordDenyComponent;

    public void refresh() {
        sensitiveWordBs.wordDeny(myWordDenyComponent).init();
    }
}
