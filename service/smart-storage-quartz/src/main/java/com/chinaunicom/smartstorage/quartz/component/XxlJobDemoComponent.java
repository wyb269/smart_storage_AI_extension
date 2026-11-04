package com.chinaunicom.smartstorage.quartz.component;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author liugang
 * @since 2025/11/20 9:41
 */
@Slf4j
@Component
public class XxlJobDemoComponent {

    @XxlJob("helloWorldHandler")
    private void helloWorld() {
        log.debug("你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好你好");;
    }
}
