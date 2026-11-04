package com.chinaunicom.smartstorage.applet.filter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Maps;
import com.liuchink.chinkutils.json.ChinkFastJson2Utils;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrBuilder;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * @author liuchink
 * @since 2024/06/21 15:37
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * 重写getParameter方法，对参数值进行HTML标签清理
     */
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        if (!CharSequenceUtil.hasEmpty(value)) {
            value = Jsoup.clean(value, Safelist.relaxed());
        }
        return value;
    }

    /**
     * 重写getParameterValues方法，对参数值数组进行HTML标签清理
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                String value = values[i];
                if (!CharSequenceUtil.hasEmpty(value)) {
                    value = Jsoup.clean(value, Safelist.relaxed());
                }
                values[i] = value;
            }
        }
        return values;
    }

    /**
     * 重写getParameterMap方法，对参数值映射进行HTML标签清理
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameters = super.getParameterMap();
        LinkedHashMap<String, String[]> map = Maps.newLinkedHashMap();
        if (parameters != null) {
            for (Map.Entry<String, String[]> stringEntry : parameters.entrySet()) {
                String[] values = parameters.get(stringEntry.getKey());
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    if (!CharSequenceUtil.hasEmpty(value)) {
                        value = Jsoup.clean(value, Safelist.relaxed());
                    }
                    values[i] = value;
                }
                map.put(stringEntry.getKey(), values);
            }
        }
        return map;
    }

    /**
     * 重写getHeader方法，对请求头进行HTML标签清理
     */
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        if (!CharSequenceUtil.hasEmpty(value)) {
            value = Jsoup.clean(value, Safelist.relaxed());
        }
        return value;
    }

    /**
     * 重写getInputStream方法，对请求体进行HTML标签清理
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        InputStream in = super.getInputStream();
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        BufferedReader buffer = new BufferedReader(reader);
        StrBuilder body = new StrBuilder();
        String line = buffer.readLine();
        while (line != null) {
            body.append(line);
            line = buffer.readLine();
        }
        buffer.close();
        reader.close();
        in.close();
        String json;
        if (CharSequenceUtil.startWith(body.toString(), "{")) {
            Map<String, Object> map = JSON.parseObject(body.toString());
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> stringObjectEntry : map.entrySet()) {
                Object val = map.get(stringObjectEntry.getKey());
                if (val instanceof String) {
                    if (!CharSequenceUtil.hasEmpty(val.toString())) {
                        result.put(stringObjectEntry.getKey(), Jsoup.clean(val.toString(), Safelist.relaxed()));
                    } else {
                        result.put(stringObjectEntry.getKey(), val.toString());
                    }
                } else {
                    result.put(stringObjectEntry.getKey(), val);
                }
            }
            json = ChinkFastJson2Utils.toJsonString(result);
        } else {
            json = body.toString();
        }
        ByteArrayInputStream bain = new ByteArrayInputStream(json.getBytes());
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return bain.read();
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }
        };
    }
}
