package com.chinaunicom.smartstorage.common.controller;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.filter.Filter;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.liuchink.chinkutils.json.ChinkFastJson2Utils;
import com.liuchink.chinkutils.net.ChinkHttpMediaType;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 基础controller
 *
 * @author liuchink
 * @since 2017/12/18 15:15
 */
@Controller("baseController")
@Slf4j
public class BaseController {
    @Resource
    protected HttpSession httpSession;
    @Resource
    protected HttpServletResponse httpServletResponse;
    @Resource
    protected HttpServletRequest httpServletRequest;
    @Resource
    protected ActionResultService actionResultService;

    /**
     * 转换成JSON字符串回写到页面
     *
     * @param object object
     * @author liuchink
     * @since 2017/05/18 11:28
     */
    protected void writerToPageByJson(Object object) {
        writerToPageByJson(ChinkFastJson2Utils.toJsonString(object));
    }

    /**
     * 转换成JSON字符串回写到页面,不打印null属性
     *
     * @param object object
     * @author liuchink
     * @since 2017/05/18 11:29
     */

    protected void writerToPageByJsonByNoNull(Object object) {
        writerToPageByJson(ChinkFastJson2Utils.toJsonStringByNoNull(object));
    }

    /**
     * 根据属性过滤器，转换成JSON字符串回写到页面
     *
     * @param object object
     * @param filter filter
     * @author liuchink
     * @since 2017/05/18 11:30
     */

    protected void writerToPageByJsonByFilter(Object object, Filter filter) {
        writerToPageByJson(ChinkFastJson2Utils.toJsonStringByFilter(object, filter));
    }

    /**
     * 根据属性过滤器，转换成JSON字符串回写到页面
     *
     * @param object object
     * @param filter filter
     * @author liuchink
     * @since 2017/05/18 11:30
     */

    protected void writerToPageByJsonByFilterByNoNull(Object object, Filter filter) {
        writerToPageByJson(ChinkFastJson2Utils.toJsonStringByFilterByNoNull(object, filter));
    }

    /**
     * 将字符串回写到页面
     *
     * @param str str
     * @author liuchink
     * @since 2017/05/18 11:31
     */

    protected void writerToPageByString(String str) {
        httpServletResponse.setContentType(ChinkHttpMediaType.TEXT_HTML.toString());
        try (ServletOutputStream servletOutputStream = httpServletResponse.getOutputStream()) {
            servletOutputStream.write(str.getBytes(StandardCharsets.UTF_8));

        } catch (IOException ioException) {
            throw new CustomException(ioException);
        }
    }

    /**
     * 将字符串回写到页面
     *
     * @param str str
     * @author liuchink
     * @since 2017/05/18 11:31
     */

    protected void writerToPageByJson(String str) {
        httpServletResponse.setContentType(ChinkHttpMediaType.APPLICATION_JSON.toString());
        try (ServletOutputStream servletOutputStream = httpServletResponse.getOutputStream()) {
            servletOutputStream.write(str.getBytes(StandardCharsets.UTF_8));

        } catch (IOException ioException) {
            throw new CustomException(ioException);
        }
    }

    public String getValidRegionId(String reqRegionId, String tokenRegionId) {
        if (CharSequenceUtil.isBlank(reqRegionId.trim()) || CharSequenceUtil.isBlank(tokenRegionId.trim())) {
            return tokenRegionId;
        }

        if ("0".equals(tokenRegionId)) {
            return reqRegionId;
        }
        if (StrUtil.length(reqRegionId) != StrUtil.length(tokenRegionId)) {
            return tokenRegionId;
        }
        String tokenPrefix = Objects.requireNonNull(tokenRegionId.trim(), "token行政区不能为空").substring(0, 2);
        String requestPrefix = Objects.requireNonNull(reqRegionId.trim(), "请求行政区不能为空").substring(0, 2);
        boolean notMatch = !"00".equals(tokenPrefix) && !tokenPrefix.equals(requestPrefix);
        if (notMatch) {
            return tokenRegionId;
        }
        tokenPrefix = tokenRegionId.substring(2, 4);
        requestPrefix = reqRegionId.substring(2, 4);
        notMatch = !"00".equals(tokenPrefix) && !tokenPrefix.equals(requestPrefix);
        if (notMatch) {
            return tokenRegionId;
        }
        tokenPrefix = tokenRegionId.substring(4, 6);
        requestPrefix = reqRegionId.substring(4, 6);
        notMatch = !"00".equals(tokenPrefix) && !tokenPrefix.equals(requestPrefix);
        if (notMatch) {
            return tokenRegionId;
        }
        tokenPrefix = tokenRegionId.substring(6, 9);
        requestPrefix = reqRegionId.substring(6, 9);
        notMatch = !"000".equals(tokenPrefix) && !tokenPrefix.equals(requestPrefix);
        if (notMatch) {
            return tokenRegionId;
        }
        tokenPrefix = tokenRegionId.substring(9);
        requestPrefix = reqRegionId.substring(9);
        notMatch = !"000".equals(tokenPrefix) && !tokenPrefix.equals(requestPrefix);
        if (notMatch) {
            return tokenRegionId;
        }
        return reqRegionId;
    }
}
