package com.chinaunicom.smartstorage.admin.service.impl.aggregation.ai;

import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation.FumigationPlanReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation.FumigationPlanRespDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation.FumigationPlanRespDTO.ConcCheck;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation.FumigationPlanRespDTO.DoseInfo;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation.FumigationPlanRespDTO.OperationStep;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation.FumigationPlanRespDTO.PointDetail;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation.FumigationPlanRespDTO.ScheduleInfo;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation.FumigationPlanRespDTO.TimelineItem;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.ai.FumigationAiService;
import com.chinaunicom.smartstorage.admin.utils.AiAskUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI辅助 - 熏蒸作业智能辅助算法实现
 * <p>算法依据：LS/T 1201-2020《磷化氢熏蒸技术规程》、GB/T 29890-2013、GB/T 22497、
 * 山西/广东省储粮技术指引（剂量与投药规则）</p>
 * <p>气体参数/药剂选择/作业信息为人工干预项：传入则覆盖算法推荐值，并在 manualOverrides 中标注。</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
@Slf4j
@Service
public class FumigationAiServiceImpl implements FumigationAiService {

    /**
     * 虫种 → 抗性分组（LS/T 1201-2020）：
     * A敏感（玉米象、长头谷盗、杂拟谷盗）/ B抗性（蛾类、谷蠹、米象、螨类、赤拟谷盗、米扁虫）/
     * C强抗性（扁谷盗属、书虱）
     */
    private static final Map<String, String> PEST_GROUP = new LinkedHashMap<>();

    static {
        PEST_GROUP.put("CORN_WEEVIL", "A");       // 玉米象
        PEST_GROUP.put("RICE_WEEVIL", "B");       // 米象
        PEST_GROUP.put("LONGHEADED_FLOUR_BEETLE", "A"); // 长头谷盗
        PEST_GROUP.put("CONFUSED_FLOUR_BEETLE", "A");   // 杂拟谷盗
        PEST_GROUP.put("RED_FLOUR_BEETLE", "B");  // 赤拟谷盗
        PEST_GROUP.put("GRAIN_BORER", "B");       // 谷蠹
        PEST_GROUP.put("MOTH", "B");              // 蛾类（麦蛾/印度谷螟）
        PEST_GROUP.put("MITE", "B");              // 螨类
        PEST_GROUP.put("FLAT_GRAIN_BEETLE", "C"); // 扁谷盗属（锈赤扁谷盗等）
        PEST_GROUP.put("BOOK_LOUSE", "C");        // 书虱
    }

    /** 粮种编码 → 中文名（方案说明文案用） */
    private static final Map<String, String> GRAIN_LABEL = new LinkedHashMap<>();

    static {
        GRAIN_LABEL.put("WHEAT", "小麦");
        GRAIN_LABEL.put("CORN", "玉米");
        GRAIN_LABEL.put("PADDY", "稻谷");
        GRAIN_LABEL.put("PADDY_JAPONICA", "粳稻");
        GRAIN_LABEL.put("RICE", "大米");
        GRAIN_LABEL.put("SOYBEAN", "大豆");
        GRAIN_LABEL.put("BARLEY", "大麦");
        GRAIN_LABEL.put("PEANUT", "花生");
        GRAIN_LABEL.put("RAPESEED", "油菜籽");
        GRAIN_LABEL.put("SESAME", "芝麻");
    }

    /** 投药点每点最大药量（g），片剂不得重叠 */
    private static final double MAX_DOSE_PER_POINT = 150.0;

    /** 散气进仓浓度限值（mL/m³） */
    private static final double AERATION_TARGET_CONC = 0.3;

    /**
     * 最低有效浓度表（mL/m³）：[虫组A/B/C][温档0/1/2][时长档14/21/28]
     * 温档：0=≥26℃ 1=21~25℃ 2=15~20℃；null表示规程表中为"—"（该组合不可行）
     */
    private static final Double[][][][] MIN_CONC = new Double[3][3][3][];

    static {
        MIN_CONC[0][0] = row(200.0, 150.0, 100.0);
        MIN_CONC[0][1] = row(250.0, 200.0, 150.0);
        MIN_CONC[0][2] = row(null, 250.0, 200.0);
        MIN_CONC[1][0] = row(300.0, 250.0, 200.0);
        MIN_CONC[1][1] = row(350.0, 300.0, 250.0);
        MIN_CONC[1][2] = row(null, 350.0, 300.0);
        MIN_CONC[2][0] = row(null, 300.0, 300.0);
        MIN_CONC[2][1] = row(null, 350.0, 300.0);
        MIN_CONC[2][2] = row(null, 400.0, 350.0);
    }

    private static Double[][] row(Double d14, Double d21, Double d28) {
        return new Double[][] {{d14}, {d21}, {d28}};
    }

    private static final int[] SEAL_DAYS = {14, 21, 28};

    @Override
    public FumigationPlanRespDTO generatePlan(FumigationPlanReqDTO reqDTO) {
        List<String> missing = checkMissingParams(reqDTO);
        if (!missing.isEmpty()) {
            FumigationPlanRespDTO ask = new FumigationPlanRespDTO();
            ask.setRequireMoreInput(true);
            ask.setMissingParams(missing);
            ask.setAiMessage(AiAskUtil.buildAskMessage("熏蒸方案", missing));
            return ask;
        }
        return generatePlanCore(reqDTO);
    }

    /**
     * 参数完整性检查：缺失项一次列全，以 AI 口吻追问，不做兜底计算
     */
    private List<String> checkMissingParams(FumigationPlanReqDTO reqDTO) {
        List<String> missing = new ArrayList<>();
        FumigationPlanReqDTO.WarehouseInfo wh = reqDTO.getWarehouseInfo() != null
            ? reqDTO.getWarehouseInfo() : new FumigationPlanReqDTO.WarehouseInfo();
        addIfBlank(missing, wh.getWarehouseType(), "仓型");
        addIfNull(missing, wh.getSpaceVolume(), "仓房空间体积");
        addIfNull(missing, wh.getGrainVolume(), "粮堆体积");
        addIfBlank(missing, wh.getAirtightLevel(), "气密性等级");
        addIfNull(missing, wh.getAirtightDecay(), "压力半衰期");
        FumigationPlanReqDTO.GrainInfo grain = reqDTO.getGrainInfo() != null
            ? reqDTO.getGrainInfo() : new FumigationPlanReqDTO.GrainInfo();
        addIfBlank(missing, grain.getGrainType(), "粮种");
        addIfNull(missing, grain.getGrainTemp(), "虫害部位最低粮温");
        FumigationPlanReqDTO.PestInfo pest = reqDTO.getPestInfo();
        if (pest == null || pest.getPestSpecies() == null || pest.getPestSpecies().isEmpty()) {
            missing.add("虫种");
        }
        FumigationPlanReqDTO.OperationInfo op = reqDTO.getOperationInfo() != null
            ? reqDTO.getOperationInfo() : new FumigationPlanReqDTO.OperationInfo();
        addIfBlank(missing, op.getOperationType(), "作业类型");
        addIfBlank(missing, op.getPlanDate(), "计划作业日期");
        addIfBlank(missing, op.getOperator(), "作业负责人");
        addIfNull(missing, op.getWorkerCount(), "作业人数");
        addIfBlank(missing, op.getOperationLevel(), "作业等级");
        FumigationPlanReqDTO.GasParamInfo gas = reqDTO.getGasParam() != null
            ? reqDTO.getGasParam() : new FumigationPlanReqDTO.GasParamInfo();
        addIfBlank(missing, gas.getGasType(), "目标气体");
        addIfNull(missing, gas.getTargetConcentration(), "目标浓度");
        addIfNull(missing, gas.getMinEffectiveConcentration(), "最低有效浓度");
        addIfNull(missing, gas.getConcentrationUniformity(), "浓度均匀要求");
        addIfNull(missing, gas.getCtRequirement(), "CT值要求");
        addIfNull(missing, gas.getSealDays(), "密闭天数");
        addIfBlank(missing, gas.getCirculationLevel(), "环流条件");
        FumigationPlanReqDTO.ChemicalInfo chem = reqDTO.getChemicalInfo() != null
            ? reqDTO.getChemicalInfo() : new FumigationPlanReqDTO.ChemicalInfo();
        addIfBlank(missing, chem.getAgentType(), "药剂类型");
        if (chem.getApplicationMethods() == null || chem.getApplicationMethods().isEmpty()) {
            missing.add("施药方式");
        }
        if (chem.getAuxiliaryMeasures() == null || chem.getAuxiliaryMeasures().isEmpty()) {
            missing.add("历史辅助措施");
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
    public FumigationPlanRespDTO generatePlanCore(FumigationPlanReqDTO reqDTO) {
        FumigationPlanReqDTO.WarehouseInfo wh = reqDTO.getWarehouseInfo();
        FumigationPlanReqDTO.GrainInfo grain = reqDTO.getGrainInfo();
        FumigationPlanReqDTO.PestInfo pest = reqDTO.getPestInfo();
        FumigationPlanReqDTO.EnvInfo env = reqDTO.getEnvInfo();
        FumigationPlanReqDTO.GasParamInfo gas = reqDTO.getGasParam();
        FumigationPlanReqDTO.ChemicalInfo chem = reqDTO.getChemicalInfo();
        FumigationPlanReqDTO.OperationInfo operation = reqDTO.getOperationInfo();

        FumigationPlanRespDTO resp = new FumigationPlanRespDTO();
        List<String> reasons = new ArrayList<>();
        List<String> overrides = new ArrayList<>();

        // 0. 环流等级人工指定覆盖（体积/粮温已要求入参必传，不再兜底推算）
        applyGasParam(wh, gas, overrides);

        // 药剂类型（默认磷化铝56%片剂）
        String agentType = chem != null && chem.getAgentType() != null
            ? chem.getAgentType() : "ALUMINIUM_PHOSPHIDE";
        boolean magnesium = "MAGNESIUM_PHOSPHIDE".equals(agentType);
        String agentLabel = magnesium ? "磷化镁" : "磷化铝";
        String agentName = agentLabel + "片剂";
        String agentSpec = magnesium ? "57%" : "56%";
        double tabletWeight = magnesium ? 3.0 : 3.2;
        List<String> methods = chem != null && chem.getApplicationMethods() != null
            ? chem.getApplicationMethods() : List.of("SURFACE");

        // 1. 前置校验（天气/气密性/粮温/种子粮水分）
        if (env != null && "雨".equals(env.getWeather())) {
            reasons.add("雨天严禁施药（磷化铝遇水易燃），请改期");
        }
        if (env != null && env.getAirRh() != null && env.getAirRh() > 85) {
            reasons.add("空气湿度过大（>85%），严禁施药");
        }
        double airtightLimit = "FLAT".equals(wh.getWarehouseType()) ? 90.0 : 60.0;
        if (wh.getAirtightDecay() != null && wh.getAirtightDecay() < airtightLimit) {
            reasons.add(String.format("气密性不达标：压力半衰期%.0fs < 要求%.0fs，需先封堵漏气部位",
                wh.getAirtightDecay(), airtightLimit));
        }
        if (grain.getGrainTemp() < 15) {
            reasons.add("粮温低于15℃，磷化氢杀虫效果差，建议采用硫酰氟熏蒸或先通风提温后再熏蒸");
        }
        if (Boolean.TRUE.equals(grain.getSeedGrain()) && grain.getGrainMoisture() != null) {
            Double seedLimit = seedMoistureLimit(grain.getGrainType());
            if (seedLimit != null && grain.getGrainMoisture() > seedLimit) {
                reasons.add(String.format("种子粮水分%.1f%%超过熏蒸上限%.1f%%，禁止熏蒸",
                    grain.getGrainMoisture(), seedLimit));
            }
        }
        if (grain.getImpurity() != null && grain.getImpurity() > 2.0) {
            reasons.add(String.format("杂质含量%.1f%%偏高（>2%%），磷化氢渗透不均且易积聚发热，建议先清理再熏蒸",
                grain.getImpurity()));
        }

        // 2. 抗性分组（多虫种取最高组）
        String group = resolveResistanceGroup(pest.getPestSpecies(), pest.getResistanceLevel());
        resp.setResistanceGroup(group);

        // 3. 密闭时长与最低有效浓度联合决策（从短到长找首个可行组合）
        int tempIndex = tempIndex(grain.getGrainTemp());
        int groupIndex = group.charAt(0) - 'A';
        int sealIndex = -1;
        Double minConc = null;
        for (int d = 0; d < SEAL_DAYS.length; d++) {
            Double conc = MIN_CONC[groupIndex][tempIndex][d][0];
            if (conc != null) {
                sealIndex = d;
                minConc = conc;
                break;
            }
        }
        if (minConc == null) {
            reasons.add("当前粮温与虫组组合在规程浓度表中无可用方案，请先升温至15℃以上");
        }
        int recommendSealDays = sealIndex >= 0 ? SEAL_DAYS[sealIndex] : 21;
        int sealDays = recommendSealDays;
        // 气体参数人工指定覆盖
        if (gas != null && gas.getMinEffectiveConcentration() != null) {
            minConc = gas.getMinEffectiveConcentration();
            overrides.add("最低有效浓度由人工指定");
        }
        resp.setMinEffectiveConcentration(minConc);
        if (gas != null && gas.getSealDays() != null && gas.getSealDays() > 0) {
            sealDays = gas.getSealDays();
            overrides.add("密闭天数由人工指定");
        }
        resp.setSealDaysBasis(String.format(
            "粮温%.0f℃、%s组害虫，查LS/T 1201-2020最低有效浓度表：密闭≥%d天需浓度%.0f mL/m³"
                + (sealDays != recommendSealDays ? "（人工指定为%d天）" : ""),
            grain.getGrainTemp(), group, recommendSealDays, minConc == null ? 0 : minConc, sealDays));

        // 4. 设定浓度（考虑抗性、粮堆吸附、仓房泄漏；气体参数可覆盖）
        double k = 1.0;
        if ("C".equals(group)) {
            k = 1.5;
        } else if ("B".equals(group)) {
            k = 1.2;
        }
        if (wh.getAirtightDecay() != null && wh.getAirtightDecay() < 120) {
            k = Math.max(k, 1.2);
        }
        double targetConc = minConc != null ? minConc * k : 0;
        if (gas != null && gas.getTargetConcentration() != null) {
            targetConc = gas.getTargetConcentration();
            overrides.add("目标浓度由人工指定");
        }
        // CT 值校验：CT = 浓度 × 密闭小时数；不足则按CT要求反算浓度
        double ctValue = targetConc * sealDays * 24.0;
        Double ctRequired = gas != null && gas.getCtRequirement() != null
            ? gas.getCtRequirement() : (minConc != null ? minConc * sealDays * 24.0 : null);
        if (ctRequired != null && ctValue < ctRequired && sealDays > 0) {
            targetConc = Math.ceil(ctRequired / (sealDays * 24.0));
            ctValue = targetConc * sealDays * 24.0;
            overrides.add("目标浓度按CT值要求反算上调");
        }
        resp.setTargetConcentration(round(targetConc, 0));
        resp.setCtValue(round(ctValue, 0));
        resp.setCtRequired(ctRequired == null ? null : round(ctRequired, 0));

        // 5. 用药量计算（GB/T 22497 剂量表）
        boolean circulation = Boolean.TRUE.equals(wh.getHasCirculation())
            || "CIRCULATION".equals(wh.getSealingMethod())
            || methods.contains("CIRCULATION");
        double dosePileUnit;
        double doseSpaceUnit;
        if (circulation) {
            // 环流/低剂量 1.5~3 g/m³，随K上浮
            dosePileUnit = Math.min(3.0, 1.5 + (k - 1.0) * 3.0);
            doseSpaceUnit = dosePileUnit * 0.6;
        } else {
            // 常规整仓：粮堆6~9、空间3~6
            dosePileUnit = 6.0 + (k - 1.0) * 6.0;
            doseSpaceUnit = 3.0 + (k - 1.0) * 6.0;
        }
        double dosePile = dosePileUnit * wh.getGrainVolume() / 1000.0;
        double doseSpace = doseSpaceUnit * wh.getSpaceVolume() / 1000.0;
        double totalDoseG = (dosePile + doseSpace) * 1000.0;
        int tablets = tabletWeight > 0 ? (int) Math.ceil(totalDoseG / tabletWeight) : 0;
        // 投药点：每点≤150g（片剂不得重叠），且不少于4点
        int points = (int) Math.max(4, Math.ceil(totalDoseG / MAX_DOSE_PER_POINT));

        DoseInfo dose = new DoseInfo();
        dose.setDosePerVolumePile(round(dosePileUnit, 1));
        dose.setDosePerVolumeSpace(round(doseSpaceUnit, 1));
        dose.setDosePile(round(dosePile, 1));
        dose.setDoseSpace(round(doseSpace, 1));
        dose.setTotalDose(round((dosePile + doseSpace), 1));
        dose.setTabletCount(tablets);
        dose.setApplicationPoints(points);
        dose.setMaxPerPoint(MAX_DOSE_PER_POINT);
        dose.setPointDetails(buildPointDetails(points, totalDoseG / points, methods, circulation));
        resp.setDose(dose);

        // 6. 取样点与散气时间
        // 浓度均匀要求≥90% 时加密取样点（默认四角+中央5点）
        int samplingPoints = gas != null && gas.getConcentrationUniformity() != null
            && gas.getConcentrationUniformity() >= 90 ? 7 : 5;
        resp.setSamplingPoints(samplingPoints);
        double grainTemp = grain.getGrainTemp();
        int aerateDays = grainTemp >= 25 ? 3 : grainTemp >= 20 ? 4 : 5;

        // 7. 时间安排
        ScheduleInfo schedule = new ScheduleInfo();
        schedule.setSealDays(sealDays);
        schedule.setAerateDays(aerateDays);
        schedule.setTotalDays(sealDays + aerateDays + 1);
        schedule.setAerationTargetConcentration(AERATION_TARGET_CONC);
        List<TimelineItem> timeline = new ArrayList<>();
        timeline.add(item("D1", "投药日",
            "封堵查漏，布设气体取样点（四角+中央，粮面下30~50cm）与试虫笼，按点施药（每点≤150g，片剂不重叠），启动环流"));
        timeline.add(item("D2~D" + sealDays, "密闭熏蒸",
            "每2天检测磷化氢浓度并记录；浓度低于设定浓度的80%时按规程补充投药"));
        timeline.add(item("D" + (sealDays + 1) + "~D" + (sealDays + aerateDays), "散气排毒",
            "开启门窗与散气口散气，仓内磷化氢残留降至0.3mL/m³以下方可进仓"));
        timeline.add(item("D" + (sealDays + aerateDays + 1) + "+", "效果检查",
            "检查试虫笼（死亡率应100%）并筛检粮堆取样；仍有活虫则分析原因重新制定方案"));
        schedule.setTimeline(timeline);
        resp.setSchedule(schedule);

        // 8. 浓度检测计划
        List<ConcCheck> checks = new ArrayList<>();
        checks.add(conc("施药后2h", "磷化氢浓度达到设定浓度的50%以上（确认环流均匀性）"));
        checks.add(conc("施药后24h", String.format("不低于设定浓度 %.0f mL/m³", targetConc)));
        checks.add(conc("之后每2天", "不低于设定浓度的80%，否则按规程补充投药"));
        checks.add(conc("散气结束", "仓内残留<0.3mL/m³，作业人员方可进仓"));
        resp.setConcentrationCheck(checks);

        // 9. 详细操作步骤（与原型 5 步一致）
        resp.setOperationSteps(buildOperationSteps(schedule, dose, points, agentName));

        // 10. 安全提示
        resp.setSafetyTips(buildSafetyTips(operation));

        // 11. 方案头信息
        resp.setAgentType(agentType);
        resp.setAgentName(agentName);
        resp.setAgentSpec(agentSpec);
        resp.setTabletWeight(tabletWeight);
        resp.setWarehouseName(wh.getWarehouseName());
        resp.setSchemeName(String.format("%s%s熏蒸方案", agentLabel, circulation ? "环流" : ""));
        resp.setSummary(buildSummary(wh, grain, pest, agentName, agentSpec, methods, points,
            dose.getTotalDose(), schedule));
        resp.setManualOverrides(overrides);
        resp.setFeasible(reasons.isEmpty());
        resp.setRejectedReasons(reasons);
        return resp;
    }

    @Override
    public String resolveResistanceGroup(List<String> pestSpecies, String resistanceLevel) {
        if ("HIGH".equals(resistanceLevel)) {
            return "C";
        }
        if ("LOW".equals(resistanceLevel)) {
            return "A";
        }
        String worst = "A";
        for (String species : pestSpecies) {
            String g = PEST_GROUP.getOrDefault(species, "B");
            if (g.compareTo(worst) > 0) {
                worst = g;
            }
        }
        return worst;
    }

    // ==================== 入参归一化 ====================

    /** 气体参数：环流等级覆盖仓库环流装置配置 */
    private void applyGasParam(FumigationPlanReqDTO.WarehouseInfo wh,
        FumigationPlanReqDTO.GasParamInfo gas, List<String> overrides) {
        if (gas != null && gas.getCirculationLevel() != null) {
            boolean circulation = "CIRCULATION".equals(gas.getCirculationLevel());
            if (!Boolean.valueOf(circulation).equals(wh.getHasCirculation())) {
                overrides.add("环流等级由人工指定");
            }
            wh.setHasCirculation(circulation);
        }
    }

    // ==================== 响应构建 ====================

    /** 投药点分布拆分：含探管施药时粮面/探管各占一半（原型「粮面3处+探管3处」） */
    private List<PointDetail> buildPointDetails(int points, double dosePerPoint,
        List<String> methods, boolean circulation) {
        List<PointDetail> details = new ArrayList<>();
        int probePoints = methods.contains("PROBE") ? Math.max(1, points / 2) : 0;
        int extraPoints = (methods.contains("BAG") ? 1 : 0) + (circulation ? 1 : 0);
        int surfacePoints = Math.max(1, points - probePoints - extraPoints);
        details.add(pointDetail("粮面", surfacePoints, dosePerPoint));
        if (probePoints > 0) {
            details.add(pointDetail("探管", probePoints, dosePerPoint));
        }
        if (methods.contains("BAG")) {
            details.add(pointDetail("气囊", 1, dosePerPoint));
        }
        if (circulation) {
            details.add(pointDetail("环流管道", 1, dosePerPoint));
        }
        return details;
    }

    private List<OperationStep> buildOperationSteps(ScheduleInfo schedule, DoseInfo dose,
        int points, String agentName) {
        int sealDays = schedule.getSealDays();
        int aerateDays = schedule.getAerateDays();
        List<OperationStep> steps = new ArrayList<>();
        steps.add(step(1, "熏蒸前准备",
            "清理仓内外杂物，检查仓房气密性，密闭门窗、通风口及各种孔洞，设置警示标识，准备防毒面具和防护用品"));
        steps.add(step(2, "布置投药点",
            String.format("%s（共%d处），每处约%.1fkg，按CFD优化位置布置，确保浓度均匀",
                pointDetailText(dose.getPointDetails()), points, dose.getTotalDose() / points)));
        steps.add(step(3, "施药与密闭",
            String.format("由持证人员佩戴防毒面具操作，投药顺序由内向外，投药后立即密闭仓门，设置警戒区域（%s）",
                agentName)));
        steps.add(step(4, "浓度监测",
            "每日检测磷化氢浓度，记录多点数据，确保浓度达标；如浓度下降过快需补药"));
        steps.add(step(5, "散气与检查",
            String.format("密闭%d天结束后开启通风散气，%d天后检测浓度<0.3ppm方可进仓；扦样检查杀虫效果，清洁仓内",
                sealDays, aerateDays)));
        return steps;
    }

    private List<String> buildSafetyTips(FumigationPlanReqDTO.OperationInfo operation) {
        List<String> tips = new ArrayList<>();
        tips.add("持证操作：必须由持证专业人员实施熏蒸作业");
        tips.add("防护装备：佩戴自给式防毒面具、穿防护服");
        tips.add("警戒区域：仓房周围20m设置警戒，禁止无关人员进入");
        int workers = operation != null && operation.getWorkerCount() != null ? operation.getWorkerCount() : 2;
        tips.add(String.format("双人作业：投药时必须两人以上同行、互相照应（方案人数 %d 人）", Math.max(workers, 2)));
        tips.add("防火防爆：磷化铝易燃爆，严禁明火、电器防爆");
        tips.add("应急预案：备好急救药品和器材，明确逃生路线");
        return tips;
    }

    private String buildSummary(FumigationPlanReqDTO.WarehouseInfo wh,
        FumigationPlanReqDTO.GrainInfo grain, FumigationPlanReqDTO.PestInfo pest, String agentName,
        String agentSpec, List<String> methods, int points, Double totalDose, ScheduleInfo schedule) {
        StringBuilder sb = new StringBuilder("针对");
        if (wh.getWarehouseName() != null && !wh.getWarehouseName().isBlank()) {
            sb.append(wh.getWarehouseName()).append("的");
        } else {
            sb.append(warehouseTypeLabel(wh.getWarehouseType()));
        }
        sb.append(GRAIN_LABEL.getOrDefault(grain.getGrainType(), grain.getGrainType()));
        if (wh.getCapacity() != null) {
            sb.append(String.format("（仓容%.0ft，", wh.getCapacity()));
        } else {
            sb.append("（");
        }
        sb.append(String.format("虫口密度%.1f头/kg，%s），采用%s%s%s施药，%d个投药点，药剂总量%.1fkg，"
                + "密闭%d天+散气%d天，预计杀虫效果95%%以上",
            pest.getPestDensity() == null ? 0.0 : pest.getPestDensity(),
            hazardLabel(pest.getHazardLevel()), agentName, agentSpec, methodText(methods), points,
            totalDose == null ? 0.0 : totalDose, schedule.getSealDays(), schedule.getAerateDays()));
        return sb.toString();
    }

    private String pointDetailText(List<PointDetail> details) {
        if (details == null || details.isEmpty()) {
            return "投药点";
        }
        StringBuilder sb = new StringBuilder();
        for (PointDetail d : details) {
            if (sb.length() > 0) {
                sb.append("+");
            }
            sb.append(d.getLocation()).append(d.getPoints()).append("处");
        }
        return sb.toString();
    }

    private String methodText(List<String> methods) {
        if (methods == null || methods.isEmpty()) {
            return "粮面";
        }
        List<String> labels = new ArrayList<>();
        for (String m : methods) {
            labels.add(switch (m) {
                case "PROBE" -> "探管";
                case "CIRCULATION" -> "环流";
                case "BAG" -> "气囊";
                default -> "粮面";
            });
        }
        return String.join("+", labels);
    }

    private String warehouseTypeLabel(String warehouseType) {
        if (warehouseType == null) {
            return "仓房";
        }
        return switch (warehouseType) {
            case "FLAT" -> "平房仓";
            case "SHALLOW" -> "浅圆仓";
            case "SILO" -> "立筒仓";
            default -> "仓房";
        };
    }

    private String hazardLabel(String hazardLevel) {
        if (hazardLevel == null) {
            return "虫害";
        }
        return switch (hazardLevel) {
            case "GENERAL" -> "一般危害";
            case "SEVERE" -> "严重危害";
            case "EXTREME" -> "特别严重危害";
            default -> "虫害";
        };
    }

    private int tempIndex(double grainTemp) {
        if (grainTemp >= 26) {
            return 0;
        }
        return grainTemp >= 21 ? 1 : 2;
    }

    private Double seedMoistureLimit(String grainType) {
        switch (grainType == null ? "" : grainType) {
            case "PADDY_JAPONICA":
                return 14.0;
            case "CORN":
            case "BARLEY":
                return 13.5;
            case "SOYBEAN":
                return 13.0;
            case "PEANUT":
                return 9.0;
            case "RAPESEED":
                return 8.0;
            case "SESAME":
                return 7.5;
            default:
                return 12.5;
        }
    }

    private TimelineItem item(String phase, String name, String desc) {
        TimelineItem i = new TimelineItem();
        i.setPhase(phase);
        i.setName(name);
        i.setDesc(desc);
        return i;
    }

    private ConcCheck conc(String time, String require) {
        ConcCheck c = new ConcCheck();
        c.setTime(time);
        c.setRequire(require);
        return c;
    }

    private PointDetail pointDetail(String location, int points, double dosePerPoint) {
        PointDetail d = new PointDetail();
        d.setLocation(location);
        d.setPoints(points);
        d.setDosePerPoint(round(dosePerPoint, 1));
        return d;
    }

    private OperationStep step(int stepNo, String title, String content) {
        OperationStep s = new OperationStep();
        s.setStepNo(stepNo);
        s.setTitle(title);
        s.setContent(content);
        return s;
    }

    private double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round(value * factor) / factor;
    }
}
