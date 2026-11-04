package com.chinaunicom.smartstorage.admin.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * 粮仓温湿度计算工具（AI 辅助算法共用）
 * <p>依据 LS/T 1202-2002《储粮机械通风技术规程》附录B：
 * 平衡水分表（表B.1）、饱和水汽压、绝对湿度、露点的查定方法。
 * 规程原为查图/查表法，实现改为公式 + 线性插值，结果与查表一致。</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
@Slf4j
public final class AiHumidityUtil {

    private AiHumidityUtil() {
    }

    /**
     * 0℃ 饱和水汽压（mmHg）
     */
    private static final double SVP_AT_ZERO_MMHG = 4.579;

    /**
     * 1 mmHg = 133.322 Pa（规程注：1mmHg≈0.133kPa）
     */
    public static final double MMHG_TO_PA = 133.322;

    /**
     * 表B.1 温度档（℃）
     */
    private static final double[] EQ_TEMP = {0, 5, 10, 15, 20, 25, 30, 35};

    /**
     * 表B.1 相对湿度档（%）
     */
    private static final double[] EQ_RH = {20, 30, 40, 50, 60, 70, 80, 90};

    /**
     * 表B.1 平衡水分（%，解吸）——粮种顺序见 GRAIN_KEYS
     * 数据来源：LS/T 1202-2002 表B.1（扫描件OCR，个别值请对照原文复核）
     */
    private static final double[][] EQ_DESORPTION_WHEAT = {
        {10.2, 11.6, 13.0, 14.3, 15.7, 17.2, 19.1, 22.3},
        {9.8, 11.1, 12.5, 13.9, 15.2, 16.7, 18.7, 21.9},
        {9.4, 10.7, 12.1, 13.4, 14.8, 16.3, 18.3, 21.4},
        {8.9, 10.3, 11.6, 13.0, 14.4, 15.9, 17.8, 21.0},
        {8.5, 9.8, 11.2, 12.6, 13.9, 15.5, 17.4, 20.5},
        {8.0, 9.4, 10.8, 12.1, 13.5, 15.0, 16.9, 20.1},
        {7.6, 8.9, 10.3, 11.7, 13.1, 14.6, 16.5, 19.7},
        {7.2, 8.5, 9.9, 11.3, 12.6, 14.1, 16.1, 19.2}};
    private static final double[][] EQ_ADSORPTION_WHEAT = {
        {7.7, 9.0, 10.4, 11.9, 13.4, 15.1, 17.1, 21.3},
        {7.4, 8.7, 10.1, 11.6, 13.1, 14.8, 16.8, 21.0},
        {7.1, 8.4, 9.8, 11.3, 12.8, 14.5, 16.5, 20.7},
        {6.8, 8.1, 9.5, 11.0, 12.5, 14.2, 16.2, 20.4},
        {6.5, 7.8, 9.2, 10.7, 12.2, 13.9, 15.8, 20.1},
        {6.2, 7.5, 8.9, 10.3, 11.9, 13.6, 15.5, 19.8},
        {5.9, 7.2, 8.6, 10.1, 11.6, 13.3, 15.2, 19.5},
        {5.6, 6.9, 8.3, 9.7, 11.3, 13.0, 14.9, 19.2}};
    private static final double[][] EQ_DESORPTION_PADDY = {
        {9.9, 11.5, 12.9, 14.2, 15.6, 17.1, 18.6, 21.1},
        {9.5, 11.1, 12.5, 13.8, 15.2, 16.6, 18.1, 20.7},
        {9.1, 10.6, 12.1, 13.4, 14.7, 16.2, 17.8, 20.2},
        {8.6, 10.2, 11.6, 12.9, 14.3, 15.7, 17.3, 19.8},
        {8.2, 9.8, 11.2, 12.5, 13.9, 15.4, 16.9, 19.4},
        {7.8, 9.4, 10.8, 12.1, 13.5, 14.9, 16.5, 19.0},
        {7.4, 9.0, 10.4, 11.7, 13.0, 14.5, 16.0, 18.5},
        {7.0, 8.6, 9.9, 11.2, 12.6, 14.1, 15.6, 18.1}};
    private static final double[][] EQ_ADSORPTION_PADDY = {
        {7.8, 9.1, 10.4, 11.7, 13.1, 14.6, 16.4, 19.3},
        {7.6, 8.9, 10.1, 11.4, 12.9, 14.3, 16.1, 19.0},
        {7.4, 8.7, 9.9, 11.2, 12.6, 14.1, 15.9, 18.8},
        {7.1, 8.4, 9.6, 11.0, 12.4, 13.8, 15.6, 18.5},
        {6.9, 8.2, 9.4, 10.7, 12.2, 13.6, 15.4, 18.3},
        {6.7, 8.0, 9.2, 10.5, 11.9, 13.4, 15.2, 18.1},
        {6.5, 7.8, 9.0, 10.3, 11.7, 13.1, 14.9, 17.8},
        {6.2, 7.5, 8.8, 10.0, 11.5, 12.9, 14.7, 17.6}};
    private static final double[][] EQ_DESORPTION_CORN = {
        {10.2, 11.5, 12.9, 14.2, 15.6, 17.0, 18.8, 21.7},
        {9.8, 11.2, 12.5, 13.9, 15.2, 16.6, 18.4, 21.3},
        {9.4, 10.8, 12.1, 13.5, 14.8, 16.2, 18.1, 20.9},
        {8.9, 10.3, 11.7, 13.0, 14.3, 15.8, 17.7, 20.5},
        {8.6, 10.0, 11.4, 12.7, 14.0, 15.5, 17.3, 20.2},
        {8.3, 9.7, 11.0, 12.3, 13.7, 15.1, 16.9, 19.8},
        {7.9, 9.3, 10.6, 12.0, 13.3, 14.7, 16.6, 19.4},
        {7.5, 8.9, 10.2, 11.6, 12.9, 14.3, 16.2, 19.0}};
    private static final double[][] EQ_ADSORPTION_CORN = {
        {8.2, 9.3, 10.5, 11.9, 13.4, 15.0, 17.4, 20.9},
        {7.9, 9.1, 10.3, 11.6, 13.1, 14.7, 17.1, 20.6},
        {7.7, 8.8, 10.1, 11.4, 12.9, 14.4, 16.9, 20.4},
        {7.4, 8.6, 9.8, 11.1, 12.6, 14.2, 16.7, 20.1},
        {7.2, 8.3, 9.5, 10.9, 12.4, 14.0, 16.3, 19.8},
        {6.9, 8.1, 9.3, 10.6, 12.1, 13.7, 16.1, 19.6},
        {6.7, 7.8, 9.0, 10.3, 11.9, 13.4, 15.8, 19.3},
        {6.5, 7.6, 8.8, 10.1, 11.6, 13.2, 15.6, 19.1}};
    private static final double[][] EQ_DESORPTION_SOYBEAN = {
        {7.3, 8.2, 9.2, 10.4, 12.2, 14.4, 18.0, 24.9},
        {6.9, 7.9, 8.9, 10.1, 11.8, 14.0, 17.7, 24.5},
        {6.6, 7.6, 8.5, 9.7, 11.5, 13.7, 17.4, 24.2},
        {6.2, 7.2, 8.2, 9.4, 11.2, 13.3, 17.1, 23.9},
        {6.0, 6.9, 8.0, 9.1, 10.9, 13.0, 16.7, 23.5},
        {5.6, 6.5, 7.5, 8.7, 10.5, 12.7, 16.4, 23.2},
        {5.3, 6.2, 7.2, 8.4, 10.1, 12.4, 16.1, 22.9},
        {5.0, 5.9, 6.8, 8.1, 9.8, 12.0, 15.8, 22.6}};
    private static final double[][] EQ_ADSORPTION_SOYBEAN = {
        {5.6, 6.6, 7.8, 9.4, 11.2, 13.5, 17.4, 24.4},
        {5.4, 6.4, 7.6, 9.2, 11.0, 13.3, 17.2, 24.2},
        {5.2, 6.2, 7.4, 8.9, 10.8, 13.1, 16.9, 24.0},
        {5.0, 6.0, 7.2, 8.7, 10.6, 12.9, 16.7, 23.8},
        {4.7, 5.7, 6.9, 8.5, 10.3, 12.6, 16.5, 23.6},
        {4.6, 5.5, 6.7, 8.3, 10.1, 12.4, 16.3, 23.4},
        {4.4, 5.4, 6.5, 8.1, 9.9, 12.2, 16.1, 23.0},
        {4.0, 5.1, 6.2, 7.9, 9.7, 12.0, 15.8, 22.6}};
    private static final double[][] EQ_DESORPTION_RICE = {
        {10.4, 11.8, 13.1, 14.5, 15.8, 17.1, 18.8, 21.1},
        {9.9, 11.5, 12.8, 14.1, 15.4, 16.7, 18.4, 20.7},
        {9.6, 11.1, 12.4, 13.7, 15.0, 16.3, 18.0, 20.4},
        {9.2, 10.7, 12.0, 13.3, 14.6, 15.9, 17.6, 20.0},
        {8.8, 10.3, 11.6, 12.9, 14.3, 15.5, 17.2, 19.5},
        {8.4, 9.9, 11.2, 12.6, 13.9, 15.2, 16.8, 19.2},
        {8.0, 9.6, 10.8, 12.2, 13.5, 14.8, 16.5, 18.8},
        {7.6, 9.1, 10.5, 11.8, 13.2, 14.4, 16.1, 18.5}};
    private static final double[][] EQ_ADSORPTION_RICE = {
        {8.7, 10.1, 11.4, 12.6, 14.0, 15.4, 17.1, 19.8},
        {8.5, 9.9, 11.1, 12.4, 13.7, 15.1, 16.8, 19.6},
        {8.3, 9.6, 10.9, 12.1, 13.4, 14.8, 16.6, 19.3},
        {8.0, 9.4, 10.6, 11.9, 13.2, 14.6, 16.3, 19.0},
        {7.7, 9.1, 10.4, 11.6, 12.9, 14.3, 16.0, 18.7},
        {7.5, 8.8, 10.1, 11.4, 12.7, 14.1, 15.8, 18.5},
        {7.2, 8.6, 9.8, 11.1, 12.4, 13.8, 15.5, 18.2},
        {7.0, 8.3, 9.6, 10.8, 12.2, 13.5, 15.2, 18.0}};

    /**
     * 粮种编码 → [解吸表, 吸附表]
     * 编码与前端/字典约定：WHEAT小麦 PADDY稻谷 RICE大米 CORN玉米 SOYBEAN大豆
     */
    private static final java.util.Map<String, double[][][]> GRAIN_TABLES = java.util.Map.of(
        "WHEAT", new double[][][]{EQ_DESORPTION_WHEAT, EQ_ADSORPTION_WHEAT},
        "PADDY", new double[][][]{EQ_DESORPTION_PADDY, EQ_ADSORPTION_PADDY},
        "CORN", new double[][][]{EQ_DESORPTION_CORN, EQ_ADSORPTION_CORN},
        "RICE", new double[][][]{EQ_DESORPTION_RICE, EQ_ADSORPTION_RICE},
        "SOYBEAN", new double[][][]{EQ_DESORPTION_SOYBEAN, EQ_ADSORPTION_SOYBEAN});

    /**
     * 大气/空气饱和水汽压（Magnus 公式，与规程附录B 表B.2 一致）
     *
     * @param tempC 温度 ℃
     * @return 饱和水汽压 mmHg
     * @author smart-storage
     * @since 2026/10/21
     */
    public static double saturationVaporPressureMmHg(double tempC) {
        return SVP_AT_ZERO_MMHG * Math.exp(17.62 * tempC / (243.12 + tempC));
    }

    /**
     * 绝对湿度（水汽分压）
     *
     * @param tempC 温度 ℃
     * @param rh 相对湿度 %
     * @return 绝对湿度 mmHg
     * @author smart-storage
     * @since 2026/10/21
     */
    public static double absoluteHumidityMmHg(double tempC, double rh) {
        return saturationVaporPressureMmHg(tempC) * rh / 100.0;
    }

    /**
     * 露点温度：由绝对湿度反解（二分法，精度0.05℃）
     *
     * @param absoluteHumidityMmHg 绝对湿度 mmHg
     * @return 露点温度 ℃
     * @author smart-storage
     * @since 2026/10/21
     */
    public static double dewPointC(double absoluteHumidityMmHg) {
        double low = -60.0;
        double high = 80.0;
        for (int i = 0; i < 60; i++) {
            double mid = (low + high) / 2;
            if (saturationVaporPressureMmHg(mid) < absoluteHumidityMmHg) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return Math.round(((low + high) / 2) * 20.0) / 20.0;
    }

    /**
     * 粮堆平衡相对湿度 RH2（%）：已知粮种、粮温、粮食水分，反查表B.1
     *
     * @param grainType 粮种编码
     * @param grainTempC 粮温 ℃
     * @param moisture 粮食水分 %
     * @param desorption true解吸（放湿，降温通风用）/ false吸附（吸湿，调质通风用）
     * @return 平衡相对湿度 %
     * @author smart-storage
     * @since 2026/10/21
     */
    public static double equilibriumRh(String grainType, double grainTempC, double moisture,
        boolean desorption) {
        double[][] table = resolveTable(grainType, desorption);
        // 先在每列RH下按粮温插值出该温度的平衡水分
        double[] moistureAtRh = new double[EQ_RH.length];
        for (int c = 0; c < EQ_RH.length; c++) {
            moistureAtRh[c] = interpolateColumn(table, c, grainTempC);
        }
        // 水分在表中区间外时按边界处理（比表值更干→RH趋近0；更湿→RH趋近100）
        if (moisture <= moistureAtRh[0]) {
            return 1.0;
        }
        if (moisture >= moistureAtRh[EQ_RH.length - 1]) {
            return 99.0;
        }
        // 在单调递增序列上反插值
        for (int c = 1; c < EQ_RH.length; c++) {
            if (moisture <= moistureAtRh[c]) {
                double m0 = moistureAtRh[c - 1];
                double m1 = moistureAtRh[c];
                double rh0 = EQ_RH[c - 1];
                double rh1 = EQ_RH[c];
                return rh0 + (moisture - m0) / (m1 - m0) * (rh1 - rh0);
            }
        }
        return 99.0;
    }

    /**
     * 粮堆平衡绝对湿度 Ps2（mmHg）
     * <p>Ps2 = RH2/100 × Pb(t2)，规程附录B B.1.2</p>
     *
     * @param grainType 粮种编码
     * @param grainTempC 粮温 ℃
     * @param moisture 粮食水分 %
     * @param desorption true解吸 / false吸附
     * @return 平衡绝对湿度 mmHg
     * @author smart-storage
     * @since 2026/10/21
     */
    public static double grainEquilibriumAbsoluteHumidityMmHg(String grainType, double grainTempC,
        double moisture, boolean desorption) {
        double rh2 = equilibriumRh(grainType, grainTempC, moisture, desorption);
        return saturationVaporPressureMmHg(grainTempC) * rh2 / 100.0;
    }

    /**
     * 粮堆温度下空气的相对湿度 RH1（规程式B.2）
     * <p>RH1 = 仓外温度下饱和水气量 × 仓外RH% ÷ 粮堆温度下饱和水气量</p>
     *
     * @param airTempC 仓外大气温度 ℃
     * @param airRh 仓外相对湿度 %
     * @param grainTempC 粮堆温度 ℃
     * @return 粮堆温度下空气相对湿度 %
     * @author smart-storage
     * @since 2026/10/21
     */
    public static double rhAtGrainTemp(double airTempC, double airRh, double grainTempC) {
        double psAir = saturationVaporPressureMmHg(airTempC) * airRh / 100.0;
        return psAir / saturationVaporPressureMmHg(grainTempC) * 100.0;
    }

    private static double[][] resolveTable(String grainType, boolean desorption) {
        double[][][] tables = GRAIN_TABLES.get(grainType);
        if (tables == null) {
            log.warn("未知粮种编码[{}]，按小麦处理", grainType);
            tables = GRAIN_TABLES.get("WHEAT");
        }
        return tables[desorption ? 0 : 1];
    }

    /**
     * 表中第 col 列（固定RH）在指定温度处的线性插值（列随温度递减，需处理非严格单调）
     */
    private static double interpolateColumn(double[][] table, int col, double tempC) {
        if (tempC <= EQ_TEMP[0]) {
            return table[0][col];
        }
        if (tempC >= EQ_TEMP[EQ_TEMP.length - 1]) {
            return table[EQ_TEMP.length - 1][col];
        }
        for (int r = 1; r < EQ_TEMP.length; r++) {
            if (tempC <= EQ_TEMP[r]) {
                double t0 = EQ_TEMP[r - 1];
                double t1 = EQ_TEMP[r];
                double v0 = table[r - 1][col];
                double v1 = table[r][col];
                return v0 + (tempC - t0) / (t1 - t0) * (v1 - v0);
            }
        }
        return table[EQ_TEMP.length - 1][col];
    }
}
