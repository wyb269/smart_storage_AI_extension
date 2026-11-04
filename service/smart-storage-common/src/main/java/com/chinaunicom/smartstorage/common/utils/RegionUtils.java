package com.chinaunicom.smartstorage.common.utils;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author liuchink
 * @since 2023/10/13 16:46
 */
@Slf4j
public class RegionUtils {

    /**
     *
     * 行政区编码分个
     *
     * @param regionId regionId
     *
     * @return java.lang.String
     * @author liuchink
     * @since 2023/10/13 17:02
     */
    public static List<String> subRegion(String regionId) {
        regionId = CharSequenceUtil.cleanBlank(regionId);
        List<String> listRegion = Lists.newArrayList();
        listRegion.add(getProvinces(regionId));
        listRegion.add(getCity(regionId));
        listRegion.add(getTown(regionId));
        listRegion.add(getCountry(regionId));
        listRegion.add(getVillage(regionId));
        return listRegion;
    }

    /**
     *
     * 行政区编码分个
     *
     * @param regionId regionId
     *
     * @return java.lang.String
     * @author liuchink
     * @since 2023/10/13 17:02
     */
    public static String subAllZero(String regionId) {
        regionId = CharSequenceUtil.cleanBlank(regionId);
        for (int length = regionId.length() - 1; length >= 0; length--) {
            if (regionId.charAt(length) != '0') {
                regionId = CharSequenceUtil.sub(regionId, 0, length + 1);
                return regionId;
            }
        }
        return "0";
    }

    /**
     *
     * 省份
     *
     * @param regionId regionId
     *
     * @return java.lang.String
     * @author liuchink
     * @since 2023/10/13 17:14
     */
    public static String getProvinces(String regionId) {
        regionId = CharSequenceUtil.cleanBlank(regionId);
        if (regionId.length() >= 2) {
            return CharSequenceUtil.sub(regionId, 0, 2);
        } else {
            return "";
        }
    }

    /**
     *
     * 城市
     *
     * @param regionId regionId
     *
     * @return java.lang.String
     * @author liuchink
     * @since 2023/10/13 17:14
     */
    public static String getCity(String regionId) {
        regionId = CharSequenceUtil.cleanBlank(regionId);
        if (regionId.length() >= 4) {
            return CharSequenceUtil.sub(regionId, 2, 4);
        } else {
            return "";
        }
    }

    /**
     *
     * 县
     *
     * @param regionId regionId
     *
     * @return java.lang.String
     * @author liuchink
     * @since 2023/10/13 17:14
     */
    public static String getTown(String regionId) {
        regionId = CharSequenceUtil.cleanBlank(regionId);
        if (regionId.length() >= 6) {
            return CharSequenceUtil.sub(regionId, 4, 6);
        } else {
            return "";
        }
    }

    /**
     *
     * 乡
     *
     * @param regionId regionId
     *
     * @return java.lang.String
     * @author liuchink
     * @since 2023/10/13 17:14
     */
    public static String getCountry(String regionId) {
        regionId = CharSequenceUtil.cleanBlank(regionId);
        if (regionId.length() >= 9) {
            return CharSequenceUtil.sub(regionId, 6, 9);
        } else {
            return "";
        }
    }

    /**
     *
     * 村
     *
     * @param regionId regionId
     *
     * @return java.lang.String
     * @author liuchink
     * @since 2023/10/13 17:14
     */
    public static String getVillage(String regionId) {
        regionId = CharSequenceUtil.cleanBlank(regionId);
        if (regionId.length() >= 12) {
            return CharSequenceUtil.sub(regionId, 9, 12);
        } else {
            return "";
        }
    }

    /**
     *
     * 行政区分割
     *
     * @param regionId regionId
     *
     * @return java.lang.String
     * @author liuchink
     * @since 2023/10/13 17:14
     */
    public static String getRegionByLevels(String regionId, int levels) {
        regionId = CharSequenceUtil.cleanBlank(regionId);
        switch (levels) {
            case 0:
                return "0";
            case 1:
                return CharSequenceUtil.sub(regionId, 0, 2);
            case 2:
                return CharSequenceUtil.sub(regionId, 0, 4);
            case 3:
                return CharSequenceUtil.sub(regionId, 0, 6);
            case 4:
                return CharSequenceUtil.sub(regionId, 0, 9);
            default:
                return CharSequenceUtil.sub(regionId, 0, 12);
        }
    }

    /**
     *
     * 行政区分割
     *
     * @param regionId regionId
     *
     * @return java.lang.String
     * @author liuchink
     * @since 2023/10/13 17:14
     */
    public static String getRegion(String regionId) {
        StringBuilder stringBuilder = new StringBuilder();
        String regionTemp = "";
        regionId = CharSequenceUtil.cleanBlank(regionId);
        regionTemp = getProvinces(regionId);
        if (!"00".equals(regionTemp)) {
            stringBuilder.append(regionTemp);
        }
        regionTemp = getCity(regionId);
        if (!"00".equals(regionTemp)) {
            stringBuilder.append(regionTemp);
        }
        regionTemp = getTown(regionId);
        if (!"00".equals(regionTemp)) {
            stringBuilder.append(regionTemp);
        }
        regionTemp = getCountry(regionId);
        if (!"000".equals(regionTemp)) {
            stringBuilder.append(regionTemp);
        }
        regionTemp = getVillage(regionId);
        if (!"000".equals(regionTemp)) {
            stringBuilder.append(regionTemp);
        }
        return stringBuilder.toString();
    }

    /**
     *
     * 省份
     *
     * @param regionId regionId
     *
     * @return java.lang.String
     * @author liuchink
     * @since 2023/10/13 17:14
     */
    public static String createProvinces(String regionId) {
        regionId = CharSequenceUtil.cleanBlank(regionId);
        return CharSequenceUtil.sub(regionId, 0, 2) + "0000000000";
    }

    /**
     *
     * 城市
     *
     * @param regionId regionId
     *
     * @return java.lang.String
     * @author liuchink
     * @since 2023/10/13 17:14
     */
    public static String createCity(String regionId) {
        regionId = CharSequenceUtil.cleanBlank(regionId);
        return CharSequenceUtil.sub(regionId, 0, 4) + "00000000";
    }

    /**
     *
     * 县
     *
     * @param regionId regionId
     *
     * @return java.lang.String
     * @author liuchink
     * @since 2023/10/13 17:14
     */
    public static String createTown(String regionId) {
        regionId = CharSequenceUtil.cleanBlank(regionId);
        return CharSequenceUtil.sub(regionId, 0, 6) + "000000";
    }

    /**
     *
     * 乡
     *
     * @param regionId regionId
     *
     * @return java.lang.String
     * @author liuchink
     * @since 2023/10/13 17:14
     */
    public static String createCountry(String regionId) {
        regionId = CharSequenceUtil.cleanBlank(regionId);
        return CharSequenceUtil.sub(regionId, 0, 9) + "000";
    }

    /**
     *
     * 村
     *
     * @param regionId regionId
     *
     * @return java.lang.String
     * @author liuchink
     * @since 2023/10/13 17:14
     */
    public static String createVillage(String regionId) {
        regionId = CharSequenceUtil.cleanBlank(regionId);
        return regionId;
    }

    public static void main(String[] args) {
        log.info("{}", getRegion("11 01 19 100 000"));
    }
}
