package com.chinaunicom.smartstorage.common.converter;

import java.util.Date;
import java.util.regex.Pattern;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;

import com.liuchink.chinkutils.date.ChinkDateFormatUtils;

import cn.hutool.core.text.CharSequenceUtil;

/**
 * 日期转换器
 *
 * @author liuchink
 * @since 2019/6/13 9:17
 */
public class DateConverter implements Converter<String, Date> {
    /**
     * 日期转换
     *
     * @param dateStr dateStr
     * @return java.util.Date
     * @author liuchink
     * @since 2019/6/13 9:36
     */
    @Override
    public Date convert(@NonNull String dateStr) {
        Date date;
        if (CharSequenceUtil.isNotBlank(dateStr)) {
            if (Pattern.matches("^\\d{4}-\\d{2}-\\d{2}$", dateStr)) {
                date = ChinkDateFormatUtils
                    .toDateTime(
                        ChinkDateFormatUtils.toDateTime(dateStr).toString(ChinkDateFormatUtils.DATEFORMAT_YYYY_MM_DD))
                    .toDate();
            } else {
                date = ChinkDateFormatUtils.toDateTime(ChinkDateFormatUtils.toDateTime(dateStr)
                    .toString(ChinkDateFormatUtils.DATEFORMAT_YYYY_MM_DD_HH_MM_SS)).toDate();
            }
            return date;
        }
        return null;
    }
}
