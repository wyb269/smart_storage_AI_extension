package com.chinaunicom.smartstorage.applet;

import cn.hutool.core.util.ReflectUtil;
import com.chinaunicom.smartstorage.applet.service.inter.aggregation.SystemConfigService;
import com.chinaunicom.smartstorage.common.config.ChinkFrameConfiguration;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 初始化
 *
 * @author liuchink
 * @since 2018/9/14 22:49
 */
@Slf4j
@Component
@Order(1)
public class SystemStart implements CommandLineRunner {
    @Resource
    private ChinkFrameConfiguration chinkFrameConfiguration;
    @Resource
    private SystemConfigService systemConfigService;

    @Override
    public void run(String... args) throws Exception {
        log.info("---------------------------------------------");
        log.info("|      {} {}       |", chinkFrameConfiguration.getProjectDisplay(),
            chinkFrameConfiguration.getProjectVersion());
        log.info("---------------------------------------------");
        log.info("--------------------系统开始初始化--------------------");
        log.info("框架版本号：{}", SystemConstant.FRAME_VERSION);
        log.info("项目虚拟路径：{}", chinkFrameConfiguration.getProjectContextPath());
        systemConfigService.cacheSystemConfig();
        log.info("系统配置缓存完成");
        log.info("--------------------系统完成初始化--------------------");
    }

    /**
     * 打印数据字典内容
     *
     * @param object object
     * @author liuchink
     * @since 2018/9/30 16:49
     */
    private void print(Object object) throws InvocationTargetException, IllegalAccessException {
        Field[] fieldArray = ReflectUtil.getFields(object.getClass());
        for (Field field : fieldArray) {
            String methodName = "get" + StringUtils.substring(field.getName(), 0, 1).toUpperCase()
                + StringUtils.substring(field.getName(), 1);
            Method method = ReflectUtil.getMethod(object.getClass(), methodName);
            if (method != null) {
                log.info("{}-->{}", field.getName(), method.invoke(object));
            }
        }
    }
}
