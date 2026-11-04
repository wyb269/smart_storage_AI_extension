package com.chinaunicom.smartstorage.admin.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;

import cn.hutool.core.date.LocalDateTimeUtil;

/**
 * 时间工具类
 *
 * @author wangluo 2025/09/05 07:20
 */
public class DateUtils {

    /**
     * 时区 - 默认
     */
    public static final String TIME_ZONE_DEFAULT = "GMT+8";
    /**
     * 秒转换成毫秒
     */
    public static final long SECOND_MILLIS = 1000;
    public static final String FORMAT_YEAR_MONTH_DAY = "yyyy-MM-dd";
    public static final String FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter FORMAT_YEAR_MONTH_DAY_FORMATTER =
                    DateTimeFormatter.ofPattern(FORMAT_YEAR_MONTH_DAY);
    public static final DateTimeFormatter FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND_FORMATTER =
                    DateTimeFormatter.ofPattern(FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND);
    /**
     * 默认时区：亚洲/上海（UTC+8，包含夏令时适配）
     */
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("M月d日");

    /**
     * 根据开始时间和总天数计算日期集合
     *
     * @param startTime 开始时间（LocalDateTime）
     * @param totalDays 总天数
     * @return 日期字符串集合（如：["10月1日", "10月2日"]）
     */
    public static List<String> calculateDateList(LocalDateTime startTime, Integer totalDays) {
        List<String> dateList = new ArrayList<>();

        // 校验参数合法性
        if (startTime == null || totalDays == null || totalDays <= 0) {
            return dateList;
        }

        // 提取开始日期（忽略时间部分）
        LocalDate startDate = startTime.toLocalDate();

        // 循环生成每一天的日期字符串
        for (int i = 0; i < totalDays; i++) {
            // 计算第i天的日期（开始日期加i天）
            LocalDate currentDate = startDate.plusDays(i);
            // 格式化日期并添加到集合
            dateList.add(currentDate.format(FORMATTER));
        }

        return dateList;
    }

    /**
     * 将 LocalDateTime 转换成 Date
     *
     * @param date LocalDateTime
     * @return LocalDateTime
     */
    public static Date of(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        // 将此日期时间与时区相结合以创建 ZonedDateTime
        ZonedDateTime zonedDateTime = date.atZone(ZoneId.systemDefault());
        // 本地时间线 LocalDateTime 到即时时间线 Instant 时间戳
        Instant instant = zonedDateTime.toInstant();
        // UTC时间(世界协调时间,UTC + 00:00)转北京(北京,UTC + 8:00)时间
        return Date.from(instant);
    }

    /**
     * 将 Date 转换成 LocalDateTime
     *
     * @param date Date
     * @return LocalDateTime
     */
    public static LocalDateTime of(Date date) {
        if (date == null) {
            return null;
        }
        // 转为时间戳
        Instant instant = date.toInstant();
        // UTC时间(世界协调时间,UTC + 00:00)转北京(北京,UTC + 8:00)时间
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static Date addTime(Duration duration) {
        return new Date(System.currentTimeMillis() + duration.toMillis());
    }

    public static boolean isExpired(LocalDateTime time) {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(time);
    }

    /**
     * 创建指定时间
     *
     * @param year 年
     * @param mouth 月
     * @param day 日
     * @return 指定时间
     */
    public static Date buildTime(int year, int mouth, int day) {
        return buildTime(year, mouth, day, 0, 0, 0);
    }

    /**
     * 创建指定时间
     *
     * @param year 年
     * @param mouth 月
     * @param day 日
     * @param hour 小时
     * @param minute 分钟
     * @param second 秒
     * @return 指定时间
     */
    public static Date buildTime(int year, int mouth, int day, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, mouth - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date max(Date a, Date b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.compareTo(b) > 0 ? a : b;
    }

    public static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    /**
     * 是否今天
     *
     * @param date 日期
     * @return 是否
     */
    public static boolean isToday(LocalDateTime date) {
        return LocalDateTimeUtil.isSameDay(date, LocalDateTime.now());
    }

    /**
     * 是否昨天
     *
     * @param date 日期
     * @return 是否
     */
    public static boolean isYesterday(LocalDateTime date) {
        return LocalDateTimeUtil.isSameDay(date, LocalDateTime.now().minusDays(1));
    }

    public static Duration getTimeToMidnight() {
        // 获取当前时间（包含时区，避免时区问题）
        ZonedDateTime now = ZonedDateTime.now();

        // 计算次日0点（当前日期+1天，时间设为00:00:00）
        ZonedDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.getZone());

        // 计算两个时间点之间的秒数
        return Duration.between(now, nextMidnight);
    }

    public static boolean isCurrentDate(String dateStr) {
        // 获取当前日期
        LocalDate currentDate = LocalDate.now();
        int currentYear = currentDate.getYear();

        // 定义日期格式器（匹配 "MM月dd日" 格式）
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM月dd日", Locale.CHINA);

        // 先解析为临时的日期访问器（仅包含月和日）
        TemporalAccessor temporal = formatter.parse(dateStr);

        // 手动补充当前年份，构建完整的LocalDate
        LocalDate targetDate = LocalDate.of(currentYear, temporal.get(java.time.temporal.ChronoField.MONTH_OF_YEAR),
                        temporal.get(java.time.temporal.ChronoField.DAY_OF_MONTH));

        // 比较当前日期与目标日期
        return currentDate.equals(targetDate);
    }

    /**
     * 将毫秒级时间戳转换为LocalDateTime（默认使用上海时区）
     *
     * @param timestamp 毫秒级时间戳（可为null）
     * @return 转换后的LocalDateTime，若timestamp为null则返回null
     */
    public static LocalDateTime convertTimestampToLocalDateTime(Long timestamp) {
        // 处理空值
        if (timestamp == null) {
            return null;
        }
        // 转换逻辑：时间戳 -> Instant -> ZonedDateTime -> LocalDateTime
        return Instant.ofEpochMilli(timestamp).atZone(DEFAULT_ZONE).toLocalDateTime();
    }

    /**
     * 将LocalDateTime转换为毫秒级时间戳（默认时区：Asia/Shanghai）
     *
     * @param localDateTime 待转换的LocalDateTime对象（null则默认当前时间）
     * @return 13位毫秒级时间戳
     */
    public static long toMilliTimestamp(LocalDateTime localDateTime) {
        // 若传入null，默认使用当前时间
        LocalDateTime dateTime = localDateTime == null ? LocalDateTime.now() : localDateTime;
        return dateTime.atZone(DEFAULT_ZONE).toInstant().toEpochMilli();
    }

}
