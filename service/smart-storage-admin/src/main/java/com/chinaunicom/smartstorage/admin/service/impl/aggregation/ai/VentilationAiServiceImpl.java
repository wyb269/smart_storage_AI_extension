package com.chinaunicom.smartstorage.admin.service.impl.aggregation.ai;

import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.ventilation.VentilationPlanReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.ventilation.VentilationPlanRespDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.ventilation.VentilationPlanRespDTO.CheckItem;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.ventilation.VentilationPlanRespDTO.CheckRow;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.ventilation.VentilationPlanRespDTO.DuctSpacingCheck;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.ventilation.VentilationPlanRespDTO.DuctVelocityCheck;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.ventilation.VentilationPlanRespDTO.ResistanceCheck;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.ventilation.VentilationPlanRespDTO.Step;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.ventilation.VentilationPlanRespDTO.VentilationWindow;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.ai.VentilationAiService;
import com.chinaunicom.smartstorage.admin.utils.AiAskUtil;
import com.chinaunicom.smartstorage.admin.utils.AiHumidityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI辅助 - 通风作业智能辅助算法实现
 * <p>算法依据：LS/T 1202-2002《储粮机械通风技术规程》第6/7/8章、附录A/B/C</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
@Slf4j
@Service
public class VentilationAiServiceImpl implements VentilationAiService {

    /** 附录A 粮层阻力粮种系数：a */
    private static final Map<String, double[]> GRAIN_A_B = new LinkedHashMap<>();

    static {
        // 粮种 -> {a, b}
        GRAIN_A_B.put("WHEAT", new double[] {681.399, 1.321});
        GRAIN_A_B.put("CORN", new double[] {414.04, 1.484});
        GRAIN_A_B.put("PADDY", new double[] {484.17, 1.334});
        GRAIN_A_B.put("RICE", new double[] {1014.129, 1.269});
        GRAIN_A_B.put("SOYBEAN", new double[] {287.514, 1.384});
        GRAIN_A_B.put("BARLEY", new double[] {534.708, 1.273});
        GRAIN_A_B.put("PEANUT", new double[] {280.414, 1.481});
    }

    /** 表1 最低单位通风量（降水通风）：水分档 -> m³/(h·t)，OCR数值待对照原文复核 */
    private static final double[] MOISTURE_STEPS = {14, 16, 18, 20, 22};
    private static final double[] MIN_UNIT_AIRFLOW = {10, 25, 30, 40, 60};

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** 空气密度（kg/m³） */
    private static final double AIR_DENSITY = 1.2;

    /** 空气比热（kJ/(kg·K)） */
    private static final double AIR_CP = 1.005;

    /** 粮食比热（kJ/(kg·K)） */
    private static final double GRAIN_CP = 1.9;

    /** 粮堆热交换效率（经验值，降温速率估算用） */
    private static final double HEAT_EXCHANGE_EFF = 0.6;

    /** 降水通风经验降水速率（%/h，待现场数据校准） */
    private static final double MOISTURE_DROP_RATE = 0.008;

    /** 峰段电价时段：08:00-11:00、18:00-23:00（左闭右开） */
    private static final int[][] PEAK_HOUR_RANGES = {{8, 11}, {18, 23}};

    /**
     * 参数完整性检查：缺失项一次列全，以 AI 口吻追问，不做兜底计算
     * <p>目标粮温仅在降温/平衡通风时必填，目标水分仅在降水/调质通风时必填</p>
     */
    private List<String> checkMissingParams(VentilationPlanReqDTO reqDTO, boolean cooling,
        boolean moistureOrConditioning) {
        List<String> missing = new ArrayList<>();
        if (reqDTO.getVentilationGoals() == null || reqDTO.getVentilationGoals().isEmpty()) {
            missing.add("通风目标");
        }
        VentilationPlanReqDTO.GrainInfo grain = reqDTO.getGrainInfo() != null
            ? reqDTO.getGrainInfo() : new VentilationPlanReqDTO.GrainInfo();
        addIfBlank(missing, grain.getWarehouseType(), "仓型");
        addIfBlank(missing, grain.getGrainType(), "粮种");
        addIfNull(missing, grain.getGrainWeight(), "粮食质量");
        addIfNull(missing, grain.getGrainVolume(), "粮堆体积");
        addIfNull(missing, grain.getBulkDensity(), "容重");
        addIfNull(missing, grain.getGrainMoisture(), "粮食水分");
        addIfNull(missing, grain.getPileHeight(), "粮堆高度");
        addIfNull(missing, grain.getGrainAvgTemp(), "平均粮温");
        VentilationPlanReqDTO.DeviceInfo device = reqDTO.getDeviceInfo() != null
            ? reqDTO.getDeviceInfo() : new VentilationPlanReqDTO.DeviceInfo();
        addIfBlank(missing, device.getFanType(), "风机类型");
        addIfBlank(missing, device.getAirSupplyMode(), "送风方式");
        addIfNull(missing, device.getFanCount(), "风机数量");
        addIfNull(missing, device.getFanAirflowPerUnit(), "单台风量");
        addIfNull(missing, device.getFanPower(), "单台功率");
        addIfNull(missing, device.getCompactionFactor(), "压紧系数");
        addIfNull(missing, device.getMainDuctArea(), "主风道面积");
        addIfNull(missing, device.getBranchDuctArea(), "支风道面积");
        addIfNull(missing, device.getDuctSpacing(), "现有风道间距");
        if (cooling) {
            addIfNull(missing, reqDTO.getTargetGrainTemp(), "目标粮温");
        }
        if (moistureOrConditioning) {
            addIfNull(missing, reqDTO.getTargetMoisture(), "目标水分");
        }
        return missing;
    }

    private void addIfNull(List<String> missing, Object value, String label) {
        if (value == null) {
            missing.add(label);
        }
    }

    private void addIfBlank(List<String> missing, String value, String label) {
        if (value == null || value.isBlank()) {
            missing.add(label);
        }
    }

    @Override
    public VentilationPlanRespDTO generatePlan(VentilationPlanReqDTO reqDTO) {
        boolean cooling = reqDTO.getVentilationGoals() != null && (reqDTO.getVentilationGoals().contains("COOLING")
            || reqDTO.getVentilationGoals().contains("BALANCE"));
        boolean moistureReduction = reqDTO.getVentilationGoals() != null
            && reqDTO.getVentilationGoals().contains("MOISTURE_REDUCTION");
        boolean conditioning = reqDTO.getVentilationGoals() != null
            && reqDTO.getVentilationGoals().contains("CONDITIONING");
        List<String> missing = checkMissingParams(reqDTO, cooling, moistureReduction || conditioning);
        if (!missing.isEmpty()) {
            VentilationPlanRespDTO ask = new VentilationPlanRespDTO();
            ask.setRequireMoreInput(true);
            ask.setMissingParams(missing);
            ask.setAiMessage(AiAskUtil.buildAskMessage("通风方案", missing));
            return ask;
        }
        VentilationPlanReqDTO.GrainInfo grain = reqDTO.getGrainInfo();
        VentilationPlanReqDTO.DeviceInfo device = reqDTO.getDeviceInfo();

        VentilationPlanRespDTO resp = new VentilationPlanRespDTO();
        resp.setSchemeName(buildSchemeName(reqDTO.getVentilationGoals()));
        List<String> reasons = new ArrayList<>();
        List<String> advices = new ArrayList<>();

        // ============ 1. 单位通风量（6.2.2） ============
        double unitAirflow;
        if (moistureReduction) {
            unitAirflow = minUnitAirflowForMoisture(grain.getGrainMoisture());
        } else if ("SILO".equals(grain.getWarehouseType())) {
            unitAirflow = 10;
        } else if ("SHALLOW".equals(grain.getWarehouseType())) {
            unitAirflow = 12;
        } else if ("EXHAUST".equals(device.getFanType())) {
            unitAirflow = 8;
        } else {
            unitAirflow = 20;
        }
        // 设备总风量：单台风量×风机数量（缺失由入口追问拦截，不再按规范推荐值兜底）
        double deviceAirflow = device.getFanAirflowPerUnit() * device.getFanCount();
        CheckItem unitCheck = new CheckItem();
        unitCheck.setValue(round(deviceAirflow / Math.max(grain.getGrainWeight(), 0.01), 1));
        unitCheck.setLimit(unitAirflow);
        unitCheck.setQualified(deviceAirflow / Math.max(grain.getGrainWeight(), 0.01) <= unitAirflow);
        unitCheck.setBasis(moistureReduction ? "降水通风最低单位通风量（表1）" : cooling
            ? "降温通风推荐单位通风量（6.2.2.2）" : "调质通风参照降温通风");
        resp.setUnitAirflow(unitCheck);
        if (Boolean.FALSE.equals(unitCheck.getQualified())) {
            reasons.add(String.format("单位通风量超过限值：实际%.1f > 限值%.1f m³/(h·t)", unitCheck.getValue(),
                unitCheck.getLimit()));
        }

        // ============ 2. 总通风量（式1）与风机参数（6.3.1） ============
        double bulkDensity = grain.getBulkDensity();
        double totalAirflow = unitAirflow * grain.getGrainVolume() * bulkDensity;
        resp.setTotalAirflow(round(totalAirflow, 0));
        double fanAirflow = totalAirflow * 1.13; // S1 = 1.10~1.16 取中值
        resp.setFanAirflow(round(fanAirflow, 0));

        // ============ 3. 粮层阻力与系统总阻力（式5/式A.1，附录A） ============
        double[] ab = GRAIN_A_B.getOrDefault(grain.getGrainType(), GRAIN_A_B.get("WHEAT"));
        double pileHeight = grain.getPileHeight();
        // 表观风速 = Q总/3600/粮堆表面积（体积/堆高近似粮堆表面积）
        double apparentVelocity = totalAirflow / 3600.0 / Math.max(grain.getGrainVolume() / pileHeight, 0.01);
        double phi = device.getCompactionFactor();
        double grainLayerResistance = 9.8 * ab[0] * pileHeight * Math.pow(apparentVelocity, ab[1]) * phi;
        ResistanceCheck resistance = new ResistanceCheck();
        resistance.setGrainLayerResistance(round(grainLayerResistance, 0));
        double resistanceLimit = cooling ? coolingResistanceLimit(pileHeight) : 1000.0;
        resistance.setLimit(resistanceLimit);
        resistance.setQualified(grainLayerResistance <= resistanceLimit);
        CheckRow detail = new CheckRow();
        detail.setName("粮层阻力计算参数");
        detail.setCondition(String.format("H=9.8·a·h·v^b·φ (a=%s, b=%s)", ab[0], ab[1]));
        detail.setActual(String.format("v表观=%.3f m/s, φ=%.2f", apparentVelocity, phi));
        detail.setPassed(resistance.getQualified());
        resistance.setDetail(detail);
        resp.setTotalResistance(resistance);
        if (Boolean.FALSE.equals(resistance.getQualified())) {
            reasons.add(String.format("粮层阻力超过限值：%.0f > %.0f Pa", grainLayerResistance, resistanceLimit));
        }

        double fanPressure = (grainLayerResistance * 1.15); // S2 = 1.1~1.2 取中值
        resp.setFanPressure(round(fanPressure, 0));

        // 装机功率：风机台数×单台功率（缺失由入口追问拦截，不再反算）
        double installedPower = device.getFanCount() * device.getFanPower();
        resp.setInstalledPower(round(installedPower, 2));

        // ============ 4. 空气途径比与风道间距（6.2.6/式6） ============
        double k = moistureReduction ? 1.35 : 1.65; // 降水1.2~1.5 / 降温1.5~1.8 取中值
        CheckItem kCheck = new CheckItem();
        kCheck.setValue(k);
        kCheck.setLimit(moistureReduction ? 1.5 : 1.8);
        kCheck.setQualified(true);
        kCheck.setBasis(moistureReduction ? "降水通风途径比宜1.2~1.5" : "降温通风途径比宜1.5~1.8");
        resp.setAirPathRatio(kCheck);

        double ductSpacing = 2 * pileHeight * (k - 1);
        DuctSpacingCheck spacing = new DuctSpacingCheck();
        spacing.setRecommended(round(ductSpacing, 1));
        spacing.setQualified(ductSpacing <= 6.0);
        resp.setDuctSpacing(spacing);

        // ============ 5. 风道风速校核（6.2.3） ============
        DuctVelocityCheck velocity = new DuctVelocityCheck();
        if (device.getMainDuctArea() != null && device.getMainDuctArea() > 0) {
            double vMain = totalAirflow / 3600.0 / device.getMainDuctArea();
            CheckItem main = new CheckItem();
            main.setValue(round(vMain, 2));
            main.setLimit(12.0);
            main.setQualified(vMain <= 12.0);
            main.setBasis("主风道风速≤12m/s，最大15m/s");
            velocity.setMain(main);
        }
        if (device.getBranchDuctArea() != null && device.getBranchDuctArea() > 0) {
            double vBranch = totalAirflow / 3600.0 / device.getBranchDuctArea();
            CheckItem branch = new CheckItem();
            branch.setValue(round(vBranch, 2));
            branch.setLimit(6.0);
            branch.setQualified(vBranch <= 6.0);
            branch.setBasis("支风道风速≤6m/s，最大9m/s");
            velocity.setBranch(branch);
        }
        resp.setDuctVelocity(velocity);

        // ============ 6. 通风时段推荐（第7章条件 + 约束条件过滤） ============
        WindowStat stat = new WindowStat();
        if (reqDTO.getWeatherForecast() != null && !reqDTO.getWeatherForecast().isEmpty()) {
            resp.setVentilationWindows(buildWindows(reqDTO, cooling, moistureReduction, conditioning, stat));
        }

        // ============ 7. 目标达成与能耗估算（对应原型结论区） ============
        fillOutcome(resp, reqDTO, cooling, moistureReduction, installedPower, totalAirflow);

        // ============ 8. 约束条件校验（允许时段/避雨湿/最长连续/峰谷电价/自然通风） ============
        resp.setConstraintChecks(buildConstraintChecks(reqDTO, stat, resp));

        // ============ 9. 历史通风效果与专项建议（历史粮情/杂质/虫害/仓房环境） ============
        buildAdvices(reqDTO, advices, resp);

        // ============ 10. 结束通风条件（7.1.2） ============
        resp.setStopConditions(buildStopConditions(reqDTO));

        // ============ 11. 操作步骤（第8章） ============
        resp.setOperationSteps(buildSteps());

        resp.setSafetyTips(List.of(
            "通风时打开仓房门窗，减少仓体压力载荷并防止气流短路（8.1.7）",
            "多台通风机应逐台启动，严禁同时启动，不得并联串联使用（8.2.2）",
            "冬季通风出现粮面结露应继续通风，辅以局部翻动（8.2.6）",
            "通风结束后关闭门窗并封闭风道口，气温回升前做好隔热密封（8.4.1）"));

        resp.setAdvices(advices);
        resp.setFeasible(reasons.isEmpty());
        resp.setRejectedReasons(reasons);
        return resp;
    }

    @Override
    public boolean allowCoolingAeration(String grainType, double grainTempC, double grainMoisture,
        double airTempC, double airRh) {
        // 温度条件（通风进行中阈值 >4℃，亚热带>3℃）
        if (grainTempC - airTempC <= 4.0) {
            return false;
        }
        // 湿度条件 Ps2 >= Ps1（解吸曲线）
        double ps2 = AiHumidityUtil.grainEquilibriumAbsoluteHumidityMmHg(grainType, grainTempC,
            grainMoisture, true);
        double ps1 = AiHumidityUtil.absoluteHumidityMmHg(airTempC, airRh);
        return ps2 >= ps1;
    }

    // ==================== private ====================

    private List<VentilationWindow> buildWindows(VentilationPlanReqDTO reqDTO, boolean cooling,
        boolean moistureReduction, boolean conditioning, WindowStat stat) {
        VentilationPlanReqDTO.GrainInfo grain = reqDTO.getGrainInfo();
        VentilationPlanReqDTO.ConstraintInfo constraint = reqDTO.getConstraintInfo();
        List<String> allowRanges = constraint == null ? null : constraint.getAllowTimeRanges();
        Double maxContinuousHours = constraint == null ? null : constraint.getMaxContinuousHours();

        List<VentilationWindow> windows = new ArrayList<>();
        List<VentilationReqPoint> passedPoints = new ArrayList<>();

        for (VentilationPlanReqDTO.WeatherPoint point : reqDTO.getWeatherForecast()) {
            // 约束1：允许通风时间窗（HH:mm-HH:mm，支持跨零点）
            if (!inAllowedRanges(point.getTime(), allowRanges)) {
                stat.filteredByTime++;
                flushWindow(windows, passedPoints, grain, maxContinuousHours, stat);
                passedPoints.clear();
                continue;
            }
            boolean weatherBlocked = isWeatherBlocked(reqDTO, point);
            List<CheckRow> checks = judgeHour(reqDTO, point, cooling, moistureReduction, conditioning);
            boolean allPassed = !weatherBlocked
                && checks.stream().allMatch(c -> Boolean.TRUE.equals(c.getPassed()));
            if (allPassed) {
                VentilationReqPoint p = new VentilationReqPoint();
                p.time = point.getTime();
                p.temp = point.getTemp();
                p.rh = point.getRh();
                p.checks = checks;
                passedPoints.add(p);
            } else {
                if (weatherBlocked) {
                    stat.filteredByRainHumidity++;
                }
                flushWindow(windows, passedPoints, grain, maxContinuousHours, stat);
                passedPoints.clear();
            }
        }
        flushWindow(windows, passedPoints, grain, maxContinuousHours, stat);

        // 排序：考虑峰谷电价（成本优先）时先按峰段小时升序，再按平均气温升序（优先低温时段）
        boolean peakValley = constraint != null && Boolean.TRUE.equals(constraint.getPeakValleyPricing());
        windows.sort((a, b) -> {
            if (peakValley) {
                int peakCompare = Integer.compare(nullSafe(a.getPeakHours()), nullSafe(b.getPeakHours()));
                if (peakCompare != 0) {
                    return peakCompare;
                }
            }
            return Double.compare(a.getAvgTemp(), b.getAvgTemp());
        });
        // 最多推荐4段
        if (windows.size() > 4) {
            windows.subList(4, windows.size()).clear();
        }
        return windows;
    }

    private void flushWindow(List<VentilationWindow> windows, List<VentilationReqPoint> points,
        VentilationPlanReqDTO.GrainInfo grain, Double maxContinuousHours, WindowStat stat) {
        if (points.isEmpty()) {
            return;
        }
        // 约束2：最长连续通风时长，超出部分自动切分为多段
        int maxPerWindow = maxContinuousHours == null || maxContinuousHours <= 0
            ? Integer.MAX_VALUE : Math.max(1, (int) Math.floor(maxContinuousHours));
        int index = 0;
        while (index < points.size()) {
            int end = Math.min(index + maxPerWindow, points.size());
            List<VentilationReqPoint> chunk = points.subList(index, end);
            VentilationWindow w = new VentilationWindow();
            w.setStartTime(chunk.get(0).time);
            w.setEndTime(chunk.get(chunk.size() - 1).time);
            w.setHours((double) chunk.size());
            w.setAvgTemp(round(chunk.stream().mapToDouble(p -> p.temp).average().orElse(0), 1));
            w.setAvgRh(round(chunk.stream().mapToDouble(p -> p.rh).average().orElse(0), 0));
            w.setTempDiff(round(grain.getGrainAvgTemp() - w.getAvgTemp(), 1));
            int peakHours = 0;
            for (VentilationReqPoint p : chunk) {
                if (isPeakHour(p.time)) {
                    peakHours++;
                }
            }
            w.setPeakHours(peakHours);
            w.setJudgement(peakHours == 0 ? "允许通风（全程谷段）" : "允许通风");
            w.setCheckItems(chunk.get(0).checks);
            windows.add(w);
            stat.totalHours += chunk.size();
            stat.peakHours += peakHours;
            index = end;
        }
    }

    /**
     * 单小时通风条件判定（规程7.1/7.2/7.3）
     */
    private List<CheckRow> judgeHour(VentilationPlanReqDTO reqDTO,
        VentilationPlanReqDTO.WeatherPoint point, boolean cooling, boolean moistureReduction,
        boolean conditioning) {
        VentilationPlanReqDTO.GrainInfo grain = reqDTO.getGrainInfo();
        List<CheckRow> checks = new ArrayList<>();
        double t2 = grain.getGrainAvgTemp();
        double t1 = point.getTemp();
        boolean subtropical = reqDTO.isSubtropical();

        if (cooling) {
            // 温度条件（进行中阈值：>4℃，亚热带>3℃）
            double tempThreshold = subtropical ? 3.0 : 4.0;
            double tempDiff = round(t2 - t1, 1);
            checks.add(checkRow("温度条件", String.format("t2-t1 > %s℃", tempThreshold),
                String.format("%.1f-%.1f=%.1f℃", t2, t1, tempDiff), tempDiff > tempThreshold));
            // 湿度条件（水分高于安全水分时判定）
            boolean needHumidityCheck = grain.getSafeMoisture() == null
                || grain.getGrainMoisture() > grain.getSafeMoisture();
            if (needHumidityCheck) {
                double ps2 = AiHumidityUtil.grainEquilibriumAbsoluteHumidityMmHg(grain.getGrainType(),
                    t2, grain.getGrainMoisture(), true);
                double ps1 = AiHumidityUtil.absoluteHumidityMmHg(t1, point.getRh());
                checks.add(checkRow("湿度条件", "Ps2 ≥ Ps1",
                    String.format("Ps2=%.2f %s Ps1=%.2f mmHg", ps2, ps2 >= ps1 ? "≥" : "<", ps1),
                    ps2 >= ps1));
            } else {
                checks.add(checkRow("湿度条件", "水分不高于安全水分时不作要求",
                    String.format("水分%.1f%%≤安全水分%.1f%%", grain.getGrainMoisture(),
                        grain.getSafeMoisture()), true));
            }
        }
        if (moistureReduction || conditioning) {
            // 温度条件：t2 > 大气露点 tL1（防结露）
            double dewPoint = AiHumidityUtil.dewPointC(AiHumidityUtil.absoluteHumidityMmHg(t1, point.getRh()));
            checks.add(checkRow("防结露条件", "t2 > 大气露点tL1",
                String.format("%.1f > %.1f℃", t2, dewPoint), t2 > dewPoint));
            // 湿度条件
            double ps1 = AiHumidityUtil.absoluteHumidityMmHg(t1, point.getRh());
            if (moistureReduction) {
                double ps1n = AiHumidityUtil.grainEquilibriumAbsoluteHumidityMmHg(grain.getGrainType(), t1,
                    grain.getGrainMoisture() - 1, true);
                checks.add(checkRow("湿度条件(降水)", "Ps' > Ps1",
                    String.format("Ps'=%.2f %s Ps1=%.2f mmHg", ps1n, ps1n > ps1 ? ">" : "≤", ps1),
                    ps1n > ps1));
            }
            if (conditioning) {
                double ps1n = AiHumidityUtil.grainEquilibriumAbsoluteHumidityMmHg(grain.getGrainType(), t1,
                    grain.getGrainMoisture() + 2.5, false);
                checks.add(checkRow("湿度条件(调质)", "Ps'' < Ps1",
                    String.format("Ps''=%.2f %s Ps1=%.2f mmHg", ps1n, ps1n < ps1 ? "<" : "≥", ps1),
                    ps1n < ps1));
            }
        }
        // 约束：避开雨天及高湿天气（相对湿度>85%）
        if (isWeatherBlocked(reqDTO, point)) {
            String weather = point.getWeather() == null ? "-" : point.getWeather();
            checks.add(checkRow("天气条件", "避开雨天及相对湿度≤85%的大气",
                String.format("天气%s、湿度%.0f%%", weather, point.getRh()), false));
        }
        return checks;
    }

    private CheckRow checkRow(String name, String condition, String actual, boolean passed) {
        CheckRow row = new CheckRow();
        row.setName(name);
        row.setCondition(condition);
        row.setActual(actual);
        row.setPassed(passed);
        return row;
    }

    private List<CheckRow> buildStopConditions(VentilationPlanReqDTO reqDTO) {
        VentilationPlanReqDTO.GrainInfo grain = reqDTO.getGrainInfo();
        boolean subtropical = reqDTO.isSubtropical();
        double tempThreshold = subtropical ? 3.0 : 4.0;
        List<CheckRow> rows = new ArrayList<>();
        // a) 温差
        Double airTemp = reqDTO.getWeatherForecast() == null || reqDTO.getWeatherForecast().isEmpty() ? null
            : reqDTO.getWeatherForecast().get(0).getTemp();
        if (airTemp != null) {
            rows.add(checkRow("温差条件", String.format("t2-t1 ≤ %s℃", tempThreshold),
                String.format("%.1f-%.1f=%.1f℃", grain.getGrainAvgTemp(), airTemp,
                    grain.getGrainAvgTemp() - airTemp),
                grain.getGrainAvgTemp() - airTemp <= tempThreshold));
        }
        // b) 温度梯度
        if (grain.getTempGradient() != null) {
            double limitDiff = "SHALLOW".equals(grain.getWarehouseType()) ? 10.0 : 3.0;
            rows.add(checkRow("温度梯度", "≤1℃/m 且上下层温差房式仓≤3℃（浅圆仓≤10℃）",
                String.format("%.1f℃/m", grain.getTempGradient()), grain.getTempGradient() <= 1.0
                    && (grain.getTopBottomTempDiff() == null || grain.getTopBottomTempDiff() <= limitDiff)));
        }
        // c) 水分梯度
        if (grain.getMoistureGradient() != null) {
            rows.add(checkRow("水分梯度", "≤0.3%/m 且上下层水分差≤1.5%",
                String.format("%.2f%%/m", grain.getMoistureGradient()),
                grain.getMoistureGradient() <= 0.3 && (grain.getTopBottomMoistureDiff() == null
                    || grain.getTopBottomMoistureDiff() <= 1.5)));
        }
        return rows;
    }

    private List<Step> buildSteps() {
        String[] texts = {
            "通风前检查风网：通风道内无积水异物，风网不漏粮（8.1.1）",
            "测定粮温、水分及大气温湿度，按7.1/7.2/7.3条件判断能否通风（8.1.5）",
            "根据通风方式打开或关闭门窗，防止气流短路（8.1.7）",
            "风机与风道口软连接并密封，防止通风机反转（8.1.6）",
            "多台风机逐台启动，运转正常后再启动下一台（8.2.2）",
            "降温通风每4h至少测温一次（降水每8h），按规程条件判定是否继续（8.3.3）",
            "通风中对不下降的死角部位插入导风管或用单管/多管机组处理（8.2.5）",
            "结束后关闭门窗、封闭风道口，气温回升前做好隔热密封（8.4.1）",
            "按附录C评估单位能耗并填写《储粮机械通风作业记录卡》（8.4.3）"};
        List<Step> steps = new ArrayList<>();
        for (int i = 0; i < texts.length; i++) {
            Step step = new Step();
            step.setOrder(i + 1);
            step.setText(texts[i]);
            steps.add(step);
        }
        return steps;
    }

    // ==================== 目标达成与能耗估算 ====================

    /**
     * 降温速率估算（℃/h）：空气显热带走热量 / 粮堆热容，取热交换效率0.6
     * <p>未提供天气预报时不做估算（返回 null），不再按粮温-4℃兜底</p>
     */
    private Double estimateCoolingRate(double actualAirflow, VentilationPlanReqDTO reqDTO) {
        if (reqDTO.getWeatherForecast() == null || reqDTO.getWeatherForecast().isEmpty()) {
            return null;
        }
        VentilationPlanReqDTO.GrainInfo grain = reqDTO.getGrainInfo();
        double grainTemp = grain.getGrainAvgTemp();
        double sum = 0;
        for (VentilationPlanReqDTO.WeatherPoint point : reqDTO.getWeatherForecast()) {
            sum += point.getTemp();
        }
        double airTemp = sum / reqDTO.getWeatherForecast().size();
        double deltaTemp = Math.max(grainTemp - airTemp, 0.5);
        double heatRemovedPerHour = actualAirflow * AIR_DENSITY * AIR_CP * deltaTemp * HEAT_EXCHANGE_EFF;
        double grainHeatCapacity = Math.max(grain.getGrainWeight() * 1000 * GRAIN_CP, 1);
        return heatRemovedPerHour / grainHeatCapacity;
    }

    /**
     * 目标达成度与能耗估算（对应原型“推荐方案”结论区）
     */
    private void fillOutcome(VentilationPlanRespDTO resp, VentilationPlanReqDTO reqDTO, boolean cooling,
        boolean moistureReduction, double installedPower, double designAirflow) {
        VentilationPlanReqDTO.GrainInfo grain = reqDTO.getGrainInfo();
        boolean naturalOnly = isNaturalOnly(reqDTO);
        resp.setTargetGrainTemp(reqDTO.getTargetGrainTemp());
        resp.setTargetMoisture(reqDTO.getTargetMoisture());

        double actualAirflow = reqDTO.getDeviceInfo().getFanAirflowPerUnit()
            * reqDTO.getDeviceInfo().getFanCount();

        Double coolingRate = null;
        if (cooling && !naturalOnly) {
            Double estimated = estimateCoolingRate(actualAirflow, reqDTO);
            coolingRate = estimated == null ? null : round(estimated, 3);
            resp.setCoolingRatePerHour(coolingRate);
        }
        if (moistureReduction && !naturalOnly) {
            resp.setMoistureDropRatePerHour(MOISTURE_DROP_RATE);
        }

        Double needTemp = reqDTO.getTargetGrainTemp() != null && cooling
            ? round(grain.getGrainAvgTemp() - reqDTO.getTargetGrainTemp(), 1) : null;
        Double needMoisture = reqDTO.getTargetMoisture() != null && moistureReduction
            ? round(grain.getGrainMoisture() - reqDTO.getTargetMoisture(), 2) : null;

        Double hoursCooling = null;
        if (needTemp != null) {
            if (needTemp <= 0) {
                hoursCooling = Double.valueOf(0);
            } else if (coolingRate != null && coolingRate > 0) {
                hoursCooling = needTemp / coolingRate;
            }
        }
        Double hoursMoisture = null;
        if (needMoisture != null) {
            hoursMoisture = needMoisture <= 0 ? Double.valueOf(0) : needMoisture / MOISTURE_DROP_RATE;
        }
        Double hours = null;
        if (!naturalOnly && (hoursCooling != null || hoursMoisture != null)) {
            hours = Math.max(hoursCooling == null ? 0d : hoursCooling,
                hoursMoisture == null ? 0d : hoursMoisture);
            hours = round(hours * effectFactor(reqDTO), 1);
        }
        resp.setEstimatedHours(hours);

        Double energy = null;
        if (naturalOnly) {
            energy = Double.valueOf(0);
        } else if (hours != null) {
            energy = round(hours * installedPower, 1);
        }
        resp.setEstimatedEnergy(energy);
        if (energy != null && energy > 0 && needTemp != null && needTemp > 0) {
            resp.setUnitEnergy(round(energy / Math.max(grain.getGrainWeight() * needTemp, 0.01), 3));
        } else if (energy != null && energy > 0 && needMoisture != null && needMoisture > 0) {
            resp.setUnitEnergy(round(energy / Math.max(grain.getGrainWeight() * needMoisture, 0.01), 3));
        }

        List<VentilationWindow> windows = resp.getVentilationWindows();
        double windowHours = windows == null ? 0
            : windows.stream().mapToDouble(w -> w.getHours() == null ? 0d : w.getHours()).sum();
        if (coolingRate != null && windowHours > 0) {
            double reachable = grain.getGrainAvgTemp() - coolingRate * windowHours;
            if (needTemp != null && needTemp > 0) {
                reachable = Math.max(reachable, grain.getGrainAvgTemp() - needTemp);
            }
            resp.setEstimatedTargetTemp(round(reachable, 1));
            if (reqDTO.getTargetGrainTemp() != null) {
                resp.setTargetReached(reachable <= reqDTO.getTargetGrainTemp() + 0.1);
            }
        } else if (reqDTO.getTargetGrainTemp() != null) {
            resp.setTargetReached(false);
        }

        // 各推荐时段的预计降温与耗电（对应原型时段卡片的“粮温降/能耗”）
        if (windows != null) {
            for (VentilationWindow window : windows) {
                double windowHoursValue = window.getHours() == null ? 0d : window.getHours();
                window.setEnergyKwh(round(naturalOnly ? 0d : windowHoursValue * installedPower, 1));
                if (coolingRate != null) {
                    double drop = coolingRate * windowHoursValue;
                    if (needTemp != null && needTemp > 0) {
                        drop = Math.min(drop, needTemp);
                    }
                    window.setTempDrop(round(drop, 1));
                }
            }
        }
        resp.setSummary(buildSummary(resp, reqDTO, naturalOnly, windowHours, needMoisture, hours));
    }

    private String buildSummary(VentilationPlanRespDTO resp, VentilationPlanReqDTO reqDTO,
        boolean naturalOnly, double windowHours, Double needMoisture, Double hours) {
        VentilationPlanReqDTO.GrainInfo grain = reqDTO.getGrainInfo();
        if (windowHours <= 0) {
            return "未来时段内无满足条件的通风窗口，建议等待气温下降或放宽约束条件后重新计算";
        }
        StringBuilder summary = new StringBuilder(String.format("利用未来%d个推荐时段（累计%.0f小时）进行%s",
            resp.getVentilationWindows().size(), windowHours, resp.getSchemeName()));
        if (resp.getEstimatedTargetTemp() != null) {
            summary.append(String.format("，预计平均粮温可由%.1f℃降至%.1f℃", grain.getGrainAvgTemp(),
                resp.getEstimatedTargetTemp()));
        }
        if (needMoisture != null && needMoisture > 0) {
            double drop = Math.min(needMoisture, windowHours * MOISTURE_DROP_RATE);
            summary.append(String.format("，水分可下降约%.1f个百分点", drop));
        }
        if (hours != null) {
            summary.append(String.format("；累计约需%.0f小时，耗电约%.0f kWh", hours,
                resp.getEstimatedEnergy() == null ? 0d : resp.getEstimatedEnergy()));
        }
        if (naturalOnly) {
            summary.append("；按零能耗自然通风执行");
        }
        if (Boolean.TRUE.equals(resp.getTargetReached())) {
            summary.append("，可达到目标");
        } else if (reqDTO.getTargetGrainTemp() != null && resp.getEstimatedTargetTemp() != null) {
            summary.append("，单次推荐时段内未达目标，需多日累计通风");
        }
        return summary.toString();
    }

    // ==================== 约束条件校验 ====================

    private List<CheckRow> buildConstraintChecks(VentilationPlanReqDTO reqDTO, WindowStat stat,
        VentilationPlanRespDTO resp) {
        List<CheckRow> rows = new ArrayList<>();
        VentilationPlanReqDTO.ConstraintInfo constraint = reqDTO.getConstraintInfo();
        if (constraint == null) {
            return rows;
        }
        if (constraint.getAllowTimeRanges() != null && !constraint.getAllowTimeRanges().isEmpty()) {
            rows.add(checkRow("允许通风时间",
                "仅在 " + String.join("、", constraint.getAllowTimeRanges()) + " 内推荐",
                String.format("已过滤时段外小时%d个", stat.filteredByTime), true));
        }
        if (Boolean.TRUE.equals(constraint.getAvoidRainHumidity())) {
            rows.add(checkRow("避开雨天及高湿天气", "剔除雨天或相对湿度>85%的小时",
                String.format("已剔除%d个不满足小时", stat.filteredByRainHumidity), true));
        }
        if (constraint.getMaxContinuousHours() != null && constraint.getMaxContinuousHours() > 0) {
            rows.add(checkRow("最长连续通风时长", String.format("≤%.0fh", constraint.getMaxContinuousHours()),
                String.format("单段最长%.0fh，超限时段已自动切分", maxWindowHours(resp)), true));
        }
        if (Boolean.TRUE.equals(constraint.getPeakValleyPricing())) {
            rows.add(checkRow("峰谷电价（成本优先）", "优先推荐谷段时段",
                String.format("推荐累计%.0fh，其中峰段%dh", stat.totalHours, stat.peakHours), true));
        }
        if (Boolean.TRUE.equals(constraint.getNaturalOnly())) {
            rows.add(checkRow("只利用自然通风", "不启动风机、能耗计0", "已按零能耗估算", true));
        }
        if (reqDTO.getTargetGrainTemp() != null && resp.getEstimatedTargetTemp() != null) {
            rows.add(checkRow("目标粮温可达性", String.format("≤%.1f℃", reqDTO.getTargetGrainTemp()),
                String.format("预计可达%.1f℃", resp.getEstimatedTargetTemp()),
                Boolean.TRUE.equals(resp.getTargetReached())));
        }
        return rows;
    }

    private double maxWindowHours(VentilationPlanRespDTO resp) {
        if (resp.getVentilationWindows() == null || resp.getVentilationWindows().isEmpty()) {
            return 0;
        }
        return resp.getVentilationWindows().stream()
            .mapToDouble(w -> w.getHours() == null ? 0d : w.getHours()).max().orElse(0);
    }

    // ==================== 历史效果与专项建议 ====================

    private void buildAdvices(VentilationPlanReqDTO reqDTO, List<String> advices,
        VentilationPlanRespDTO resp) {
        VentilationPlanReqDTO.GrainInfo grain = reqDTO.getGrainInfo();
        VentilationPlanReqDTO.HistoryInfo history = reqDTO.getHistoryInfo();
        if (history != null) {
            if (history.getVentilationTimesLastYear() != null) {
                int times = history.getVentilationTimesLastYear();
                advices.add(times >= 3
                    ? String.format("近一年已通风%d次，频次偏高，建议检查仓房气密性与隔热性能", times)
                    : String.format("近一年通风%d次，通风频次正常", times));
            }
            if (history.getLastYearGrainTemp() != null) {
                double diff = round(grain.getGrainAvgTemp() - history.getLastYearGrainTemp(), 1);
                if (diff >= 1.5) {
                    advices.add(String.format("当前粮温较去年同期高%.1f℃，建议提前安排降温通风", diff));
                } else if (diff <= -1.5) {
                    advices.add(String.format("当前粮温较去年同期低%.1f℃，通风压力较小", -diff));
                }
            }
            if (history.getLastYearMoisture() != null) {
                double diff = round(grain.getGrainMoisture() - history.getLastYearMoisture(), 2);
                if (Math.abs(diff) >= 0.5) {
                    advices.add(String.format("当前水分较去年同期%s%.2f个百分点，%s", diff > 0 ? "高" : "低",
                        Math.abs(diff), diff > 0 ? "注意降水通风" : "注意防止过度失水"));
                }
            }
            if (history.getLastTempBefore() != null && history.getLastTempAfter() != null) {
                double lastDrop = round(history.getLastTempBefore() - history.getLastTempAfter(), 1);
                advices.add(String.format("上次通风实测降温%.1f℃%s，可作为本次时长修正依据", lastDrop,
                    history.getLastVentilationHours() == null ? ""
                        : String.format("、耗时%.0fh", history.getLastVentilationHours())));
            }
            if (history.getLastEnergyKwh() != null && history.getLastVentilationHours() != null
                && history.getLastVentilationHours() > 0) {
                double avgPower = round(history.getLastEnergyKwh() / history.getLastVentilationHours(), 1);
                advices.add(String.format("上次通风平均功率%.1f kW、能耗%.0f kWh，本次风机配置可与之对标",
                    avgPower, history.getLastEnergyKwh()));
            }
            String rating = history.getLastEffectRating();
            if ("BAD".equalsIgnoreCase(rating) || "较差".equals(rating)) {
                advices.add("上次通风效果较差：本次预计时长已上浮20%，建议核查风道布置、死角与气流短路问题，必要时提高单位通风量");
            } else if ("GOOD".equalsIgnoreCase(rating) || "良好".equals(rating)) {
                advices.add("上次通风效果良好：本次预计时长按90%折算，可沿用上次通风参数");
            }
        }
        if (reqDTO.getWarehouseRh() != null && reqDTO.getWarehouseRh() > 75) {
            advices.add(String.format("仓内湿度%.0f%%偏高，通风前先排除仓内湿热空气并检查防潮层",
                reqDTO.getWarehouseRh()));
        }
        if (reqDTO.getWarehouseTemp() != null
            && reqDTO.getWarehouseTemp() - grain.getGrainAvgTemp() > 5) {
            advices.add("仓温明显高于粮温，粮面存在结露风险，建议选择低温时段通风并监测粮面");
        }
        if (Boolean.FALSE.equals(resp.getTargetReached()) && reqDTO.getTargetGrainTemp() != null
            && resp.getEstimatedTargetTemp() != null) {
            advices.add("单次推荐时段内难以达到目标粮温，建议延长通风天数、提高单位通风量或等待更强冷空气");
        }
    }

    // ==================== 约束与时间工具 ====================

    private boolean isNaturalOnly(VentilationPlanReqDTO reqDTO) {
        return reqDTO.getConstraintInfo() != null
            && Boolean.TRUE.equals(reqDTO.getConstraintInfo().getNaturalOnly());
    }

    /** 历史通风效果修正系数：良好 0.9、较差 1.2、其余 1.0 */
    private double effectFactor(VentilationPlanReqDTO reqDTO) {
        if (reqDTO.getHistoryInfo() == null) {
            return 1.0;
        }
        String rating = reqDTO.getHistoryInfo().getLastEffectRating();
        if ("GOOD".equalsIgnoreCase(rating) || "良好".equals(rating)) {
            return 0.9;
        }
        if ("BAD".equalsIgnoreCase(rating) || "较差".equals(rating)) {
            return 1.2;
        }
        return 1.0;
    }

    /** 是否命中“避开雨天及高湿天气”约束 */
    private boolean isWeatherBlocked(VentilationPlanReqDTO reqDTO, VentilationPlanReqDTO.WeatherPoint point) {
        if (reqDTO.getConstraintInfo() == null
            || !Boolean.TRUE.equals(reqDTO.getConstraintInfo().getAvoidRainHumidity())) {
            return false;
        }
        String weather = point.getWeather();
        boolean rain = weather != null && (weather.contains("雨") || "RAIN".equalsIgnoreCase(weather));
        return rain || point.getRh() != null && point.getRh() > 85;
    }

    /** 时间点是否落在允许通风时间窗内（HH:mm-HH:mm，支持跨零点；空表示不限制） */
    private boolean inAllowedRanges(String time, List<String> ranges) {
        if (ranges == null || ranges.isEmpty()) {
            return true;
        }
        LocalDateTime dateTime = parseDateTime(time);
        if (dateTime == null) {
            return true;
        }
        int minutes = dateTime.getHour() * 60 + dateTime.getMinute();
        for (String range : ranges) {
            if (range == null) {
                continue;
            }
            String[] parts = range.split("[-~—]");
            if (parts.length != 2) {
                continue;
            }
            Integer start = parseClockMinutes(parts[0].trim());
            Integer end = parseClockMinutes(parts[1].trim());
            if (start == null || end == null) {
                continue;
            }
            if (start <= end) {
                if (minutes >= start && minutes <= end) {
                    return true;
                }
            } else if (minutes >= start || minutes <= end) {
                return true;
            }
        }
        return false;
    }

    private Integer parseClockMinutes(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String[] parts = text.split(":");
        if (parts.length < 2) {
            return null;
        }
        try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            return hour * 60 + minute;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, TIME_FORMAT);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    /** 是否处于峰段电价时段（08:00-11:00、18:00-23:00） */
    private boolean isPeakHour(String time) {
        LocalDateTime dateTime = parseDateTime(time);
        if (dateTime == null) {
            return false;
        }
        int hour = dateTime.getHour();
        for (int[] range : PEAK_HOUR_RANGES) {
            if (hour >= range[0] && hour < range[1]) {
                return true;
            }
        }
        return false;
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private double minUnitAirflowForMoisture(double moisture) {
        for (int i = 0; i < MOISTURE_STEPS.length; i++) {
            if (moisture <= MOISTURE_STEPS[i]) {
                return MIN_UNIT_AIRFLOW[i];
            }
        }
        return MIN_UNIT_AIRFLOW[MIN_UNIT_AIRFLOW.length - 1];
    }

    private double coolingResistanceLimit(double pileHeight) {
        if (pileHeight < 6) {
            return 750;
        }
        if (pileHeight <= 15) {
            return 2000;
        }
        return 2400;
    }

    private String buildSchemeName(List<String> goals) {
        StringBuilder name = new StringBuilder("机械通风");
        if (goals.contains("MOISTURE_REDUCTION")) {
            name.append("降水");
        }
        if (goals.contains("COOLING") || goals.contains("BALANCE")) {
            name.append("降温");
        }
        if (goals.contains("CONDITIONING")) {
            name.append("调质");
        }
        return name.append("方案").toString();
    }

    private double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round(value * factor) / factor;
    }

    /** 内部：通过判定的天气点 */
    private static class VentilationReqPoint {
        String time;
        double temp;
        double rh;
        List<CheckRow> checks;
    }

    /** 内部：约束条件过滤统计 */
    private static class WindowStat {
        /** 被“允许通风时间窗”剔除的小时数 */
        int filteredByTime;
        /** 被“避开雨天及高湿”剔除的小时数 */
        int filteredByRainHumidity;
        /** 推荐时段累计小时数 */
        double totalHours;
        /** 推荐时段中峰段累计小时数 */
        int peakHours;
    }
}
