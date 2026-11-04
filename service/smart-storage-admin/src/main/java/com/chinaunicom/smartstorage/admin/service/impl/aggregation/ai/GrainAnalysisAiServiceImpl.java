package com.chinaunicom.smartstorage.admin.service.impl.aggregation.ai;

import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.grain.GrainAnalysisReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.grain.GrainAnalysisRespDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.grain.GrainAnalysisRespDTO.TreatPlan;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.grain.GrainAnalysisRespDTO.Warning;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.ai.GrainAnalysisAiService;
import com.chinaunicom.smartstorage.admin.utils.AiAskUtil;
import com.chinaunicom.smartstorage.admin.utils.AiHumidityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI辅助 - 粮情智能分析算法实现
 * <p>四维度加权评分（粮温30/虫害30/水分25/环境15 + 历史加成×1.15），
 * 权重与阈值为建议初值（对应原型示例78分=高风险），后续迁移到字典表配置</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
@Slf4j
@Service
public class GrainAnalysisAiServiceImpl implements GrainAnalysisAiService {

    private static final double WEIGHT_TEMP = 30;
    private static final double WEIGHT_PEST = 30;
    private static final double WEIGHT_MOISTURE = 25;
    private static final double WEIGHT_ENV = 15;

    /** 默认预测周期（天），对应原型「未来15天」 */
    private static final int DEFAULT_FORECAST_DAYS = 15;

    @Override
    public GrainAnalysisRespDTO analyze(GrainAnalysisReqDTO reqDTO) {
        List<String> missing = checkMissingParams(reqDTO);
        if (!missing.isEmpty()) {
            GrainAnalysisRespDTO ask = new GrainAnalysisRespDTO();
            ask.setRequireMoreInput(true);
            ask.setMissingParams(missing);
            ask.setAiMessage(AiAskUtil.buildAskMessage("粮情分析", missing));
            return ask;
        }
        GrainAnalysisRespDTO resp = new GrainAnalysisRespDTO();
        int forecastDays = reqDTO.getForecastDays() != null && reqDTO.getForecastDays() > 0
            ? reqDTO.getForecastDays() : DEFAULT_FORECAST_DAYS;
        double avgTemp = reqDTO.getGrainInfo().getGrainAvgTemp();

        List<Warning> warnings = new ArrayList<>();
        Warning temp = evalTemp(reqDTO, avgTemp, forecastDays);
        Warning pest = evalPest(reqDTO);
        Warning moisture = evalMoisture(reqDTO);
        Warning env = evalEnv(reqDTO, avgTemp);
        // 历史加成：该项曾发生且再次预警时 ×1.15（封顶权重）
        applyHistoryBoost(temp, "MOLD", reqDTO);
        applyHistoryBoost(pest, "PEST", reqDTO);
        applyHistoryBoost(env, "CONDENSATION", reqDTO);
        warnings.add(temp);
        warnings.add(pest);
        warnings.add(moisture);
        warnings.add(env);

        double total = Math.min(100, Math.round(
            (temp.getScore() + pest.getScore() + moisture.getScore() + env.getScore()) * 10) / 10.0);
        resp.setRiskScore(total);
        String level = total >= 70 ? "HIGH" : total >= 40 ? "MEDIUM" : "LOW";
        resp.setRiskLevel(level);
        resp.setRiskLevelText("HIGH".equals(level) ? "高风险" : "MEDIUM".equals(level) ? "中风险" : "低风险");

        boolean anySevere = warnings.stream().anyMatch(w -> "SEVERE".equals(w.getLevel()));
        boolean anyModerate = warnings.stream().anyMatch(w -> "MODERATE".equals(w.getLevel()));
        String adviceLevel = "HIGH".equals(level) || anySevere ? "immediate"
            : "MEDIUM".equals(level) || anyModerate ? "plan" : "monitor";
        resp.setAdviceLevel(adviceLevel);
        resp.setWarnings(warnings);
        resp.setForecastDays(forecastDays);
        resp.setSummary(buildSummary(reqDTO, warnings, level, forecastDays, avgTemp));

        List<TreatPlan> treatPlans = buildTreatPlans(reqDTO, warnings, adviceLevel);
        resp.setTreatPlans(treatPlans);
        resp.setTreatPlan(treatPlans.isEmpty() ? null : treatPlans.get(0));
        return resp;
    }

    /**
     * 参数完整性检查：缺失项一次列全，以 AI 口吻追问，不做兜底推算
     */
    private List<String> checkMissingParams(GrainAnalysisReqDTO reqDTO) {
        List<String> missing = new ArrayList<>();
        GrainAnalysisReqDTO.GrainInfo g = reqDTO.getGrainInfo() != null
            ? reqDTO.getGrainInfo() : new GrainAnalysisReqDTO.GrainInfo();
        if (g.getGrainType() == null || g.getGrainType().isBlank()) {
            missing.add("粮种");
        }
        addIfNull(missing, g.getGrainMoisture(), "粮食水分");
        addIfNull(missing, g.getSafeMoisture(), "当地储粮安全水分");
        addIfNull(missing, g.getGrainAvgTemp(), "平均粮温");
        GrainAnalysisReqDTO.EnvData e = reqDTO.getEnvData() != null
            ? reqDTO.getEnvData() : new GrainAnalysisReqDTO.EnvData();
        addIfNull(missing, e.getWarehouseTemp(), "仓温");
        addIfNull(missing, e.getWarehouseRh(), "仓湿");
        return missing;
    }

    private void addIfNull(List<String> missing, Object value, String label) {
        if (value == null) {
            missing.add(label);
        }
    }

    /** 粮温异常项（0~30分）：含预测期内升温趋势判定 */
    private Warning evalTemp(GrainAnalysisReqDTO reqDTO, double avgTemp, int forecastDays) {
        GrainAnalysisReqDTO.MonitorData m = reqDTO.getMonitorData();
        GrainAnalysisReqDTO.GrainInfo g = reqDTO.getGrainInfo();
        Warning w = base("TEMP_ABNORMAL", "粮温异常过高");
        int hotSpots = m != null && m.getHotSpotCount() != null ? m.getHotSpotCount() : 0;
        double hotTemp = m != null && m.getHotSpotMaxTemp() != null ? m.getHotSpotMaxTemp() : 0;
        Double riseRate = m != null ? m.getTempRiseRate() : null;
        Double tbDiff = m != null ? m.getTopBottomTempDiff() : null;
        // 分层温差可由上/下层温度推算
        if (tbDiff == null && g.getTopLayerTemp() != null && g.getBottomLayerTemp() != null) {
            tbDiff = round(Math.abs(g.getTopLayerTemp() - g.getBottomLayerTemp()), 1);
        }
        double maxTemp = g.getGrainMaxTemp() != null ? g.getGrainMaxTemp() : avgTemp;
        // 预测期末最高粮温
        double predictedMax = round(maxTemp + (riseRate != null ? riseRate * forecastDays : 0), 1);
        if (hotSpots == 0 && avgTemp <= 25 && predictedMax <= 28) {
            w.setDescription(String.format("粮温正常，无异常高温点，预计%d天后最高粮温%.1f℃", forecastDays, predictedMax));
            return w;
        }
        boolean moderate = hotTemp > 28 && hotTemp <= 30 || predictedMax > 28
            || riseRate != null && riseRate > 0.5 && riseRate <= 1.0 || tbDiff != null && tbDiff > 8;
        boolean severe = hotTemp > 30 || predictedMax > 30 || riseRate != null && riseRate > 1.0 || hotSpots >= 3;
        String level = severe ? "SEVERE" : moderate ? "MODERATE" : "MILD";
        w.setLevel(level);
        w.setLevelText(levelText(level));
        w.setScore("SEVERE".equals(level) ? WEIGHT_TEMP : "MODERATE".equals(level) ? 20.0 : 10.0);
        w.setDescription(String.format("检测到%d处异常高温点，最高%.1f℃，升温速率%s℃/天，预计%d天后最高粮温升至%.1f℃",
            hotSpots, hotTemp, riseRate == null ? "-" : riseRate.toString(), forecastDays, predictedMax));
        w.setImpact("持续发热可能导致粮堆结露、品质劣变，并向周边扩散，常与虫害滋生互为因果");
        w.setSuggestions(List.of(severe ? "立即查明发热原因（虫害/水分/杂质区）并处置" : "查明发热原因（虫害/水分/杂质区）",
            "采用单管/多管风机对发热点局部通风降温",
            severe ? "检测频次加密至每日1次" : "增加检测频次至每2日1次"));
        w.setRemark(predictedMax > 30 ? "需立即处置" : predictedMax > 28 ? "需注意" : null);
        return w;
    }

    /** 害虫项（0~30分，GB/T 29890 虫粮等级映射） */
    private Warning evalPest(GrainAnalysisReqDTO reqDTO) {
        GrainAnalysisReqDTO.MonitorData m = reqDTO.getMonitorData();
        Warning w = base("PEST", "害虫密度超标");
        double total = m != null && m.getPestDensity() != null ? m.getPestDensity() : 0;
        double main = m != null && m.getMainPestDensity() != null ? m.getMainPestDensity() : 0;
        if (total <= 5 && main <= 2) {
            w.setDescription(String.format("基本无虫粮：总密度%.0f、主要害虫%.0f头/kg", total, main));
            return w;
        }
        if (total <= 30 && main <= 10) {
            w.setLevel("MODERATE");
            w.setScore(WEIGHT_PEST / 2);
            w.setDescription(String.format("一般虫粮：总密度%.0f头/kg、主要害虫%.0f头/kg", total, main));
            w.setImpact("粮温≥15℃时害虫将快速繁殖，15天内需除治");
            w.setSuggestions(List.of("发起虫害分析确定除治方案", "粮温≥15℃时15天内完成除治"));
        } else {
            w.setLevel("SEVERE");
            w.setScore(WEIGHT_PEST);
            w.setDescription(String.format("严重虫粮：总密度%.0f头/kg、主要害虫%.0f头/kg", total, main));
            w.setImpact("害虫大量繁殖将造成储粮损失与粮温异常，7天内必须除治");
            w.setSuggestions(List.of("7天内除治（GB/T 29890 严重虫粮）", "立即发起虫害分析生成熏蒸方案"));
            w.setRemark("需立即处置");
        }
        w.setLevelText(levelText(w.getLevel()));
        return w;
    }

    /** 水分项（0~25分） */
    private Warning evalMoisture(GrainAnalysisReqDTO reqDTO) {
        GrainAnalysisReqDTO.GrainInfo g = reqDTO.getGrainInfo();
        GrainAnalysisReqDTO.MonitorData m = reqDTO.getMonitorData();
        Warning w = base("MOISTURE", "水分含量偏高");
        double safe = g.getSafeMoisture();
        double over = g.getGrainMoisture() - safe;
        Double tbDiff = m != null ? m.getTopBottomMoistureDiff() : null;
        if (over <= 0) {
            w.setDescription(String.format("平均水分%.1f%%未超安全水分%.1f%%", g.getGrainMoisture(), safe));
            return w;
        }
        boolean moderate = over > 0.5 || tbDiff != null && tbDiff > 1.5;
        boolean severe = over > 1.5;
        String level = severe ? "SEVERE" : moderate ? "MODERATE" : "MILD";
        w.setLevel(level);
        w.setScore("SEVERE".equals(level) ? WEIGHT_MOISTURE : "MODERATE".equals(level) ? 15.0 : 8.0);
        w.setDescription(String.format("平均水分%.1f%%，超出安全水分%.1f%%的%.1f个百分点", g.getGrainMoisture(), safe,
            over));
        w.setImpact("水分超标在粮温>20℃时霉变风险显著上升，并利于害虫繁殖");
        w.setSuggestions(List.of(severe ? "立即通风降水或倒仓处理" : "择机通风降水（先通过通风辅助判定可行性）",
            "检测水分梯度，排查局部高水分区"));
        w.setLevelText(levelText(level));
        return w;
    }

    /** 仓内环境项（0~15分，含结露风险判定） */
    private Warning evalEnv(GrainAnalysisReqDTO reqDTO, double grainTemp) {
        GrainAnalysisReqDTO.EnvData e = reqDTO.getEnvData();
        Warning w = base("ENVIRONMENT", "仓内环境风险");
        double whRh = e.getWarehouseRh();
        double whTemp = e.getWarehouseTemp();
        double ps2 = AiHumidityUtil.grainEquilibriumAbsoluteHumidityMmHg(
            reqDTO.getGrainInfo().getGrainType(), grainTemp,
            reqDTO.getGrainInfo().getGrainMoisture(), true);
        double dewPoint = AiHumidityUtil.dewPointC(ps2);
        boolean condensation = whTemp <= dewPoint;
        if (whRh <= 70 && !condensation) {
            w.setDescription(String.format("仓温%.1f℃、仓湿%.0f%%，无结露风险", whTemp, whRh));
            return w;
        }
        boolean moderate = whRh > 75 || condensation;
        boolean severe = condensation && Math.abs(whTemp - grainTemp) > 8;
        String level = severe ? "SEVERE" : moderate ? "MODERATE" : "MILD";
        w.setLevel(level);
        w.setScore("SEVERE".equals(level) ? WEIGHT_ENV : "MODERATE".equals(level) ? 10.0 : 5.0);
        w.setDescription(String.format("仓温%.1f℃、仓湿%.0f%%；粮堆露点%.1f℃%s", whTemp, whRh, dewPoint,
            condensation ? "，仓温低于露点存在结露风险" : ""));
        w.setImpact("高湿环境加速粮堆吸湿与霉菌活动，结露将引发局部水分骤升");
        w.setSuggestions(List.of(condensation ? "避开高湿时段开仓，必要时用排风扇排除仓内空间湿热" : "关注仓湿变化趋势",
            whRh > 75 ? "检查仓房密闭与防潮层，投放吸湿剂" : "维持常规环境管理"));
        w.setLevelText(levelText(level));
        w.setRemark(condensation ? "需注意" : null);
        return w;
    }

    /** 风险摘要（原型等级卡片下方一句话结论） */
    private String buildSummary(GrainAnalysisReqDTO reqDTO, List<Warning> warnings, String level,
        int forecastDays, double avgTemp) {
        List<Warning> abnormal = warnings.stream()
            .filter(w -> !"NORMAL".equals(w.getLevel())).toList();
        if (abnormal.isEmpty()) {
            return String.format("当前各指标正常，粮温%.1f℃，预计%d天内无显著风险，维持常规检测即可", avgTemp,
                forecastDays);
        }
        String factors = String.join("、", abnormal.stream().map(Warning::getName).toList());
        boolean tempRisk = abnormal.stream().anyMatch(w -> "TEMP_ABNORMAL".equals(w.getType()));
        String head = tempRisk
            ? String.format("预测%d天后粮温将接近预警线", forecastDays)
            : String.format("当前粮温%.1f℃，未超预警值", avgTemp);
        return String.format("%s，叠加%s因素，存在%s粮情风险；共%d项指标需处置",
            head, factors, "HIGH".equals(level) ? "较大" : "一定", abnormal.size());
    }

    /** 处置方案推荐（规则联动其他模块，最多返回 3 条，按优先级排序） */
    private List<TreatPlan> buildTreatPlans(GrainAnalysisReqDTO reqDTO, List<Warning> warnings,
        String adviceLevel) {
        List<TreatPlan> plans = new ArrayList<>();
        Warning temp = findWarning(warnings, "TEMP_ABNORMAL");
        Warning pest = findWarning(warnings, "PEST");
        Warning moisture = findWarning(warnings, "MOISTURE");
        Warning env = findWarning(warnings, "ENVIRONMENT");

        boolean pestRisk = isRisk(pest);
        boolean tempRisk = isRisk(temp);
        boolean moistureRisk = isRisk(moisture);
        boolean envRisk = isRisk(env);
        boolean immediate = "immediate".equals(adviceLevel);

        if (pestRisk) {
            plans.add(plan(plans.size() + 1, "FUMIGATION",
                (immediate ? "立即安排" : "建议安排") + "熏蒸杀虫",
                String.format("虫粮等级已达%s，应在%s内除治",
                    pest.getDescription(), describeDeadline(pest.getLevel())),
                "采用磷化铝熏蒸，建议用药量6g/m³，密闭7天以上；详见「熏蒸辅助」功能生成完整方案",
                "/fumigation", "详见「熏蒸辅助」功能"));
        }
        if (tempRisk || moistureRisk) {
            boolean nightOk = reqDTO.getEnvData().getAirTemp() != null
                && reqDTO.getGrainInfo().getGrainAvgTemp() - reqDTO.getEnvData().getAirTemp() > 4;
            plans.add(plan(plans.size() + 1, "VENTILATION",
                tempRisk ? "夜间通风降温" + (moistureRisk ? "降水" : "") : "择机通风降水",
                tempRisk ? "存在异常高温点，需降低粮温防止发热扩散" : "水分超出安全水分，需通风降水降低霉变风险",
                nightOk
                    ? "利用夜间低温低湿时段开启轴流风机或自然通风，降低粮堆温度和水分；详见「通风辅助」功能"
                    : "当前大气条件暂不满足降温通风（粮温-气温需>4℃），待冷空气过境后择机通风；详见「通风辅助」功能",
                "/ventilation", "详见「通风辅助」功能"));
        }
        if (envRisk) {
            plans.add(plan(plans.size() + 1, "MIXED", "管控仓内湿热防结露",
                env.getDescription(),
                "高湿时段关闭门窗，必要时用排风扇排除仓内湿热，并检查仓房防潮层",
                null, null));
        }
        plans.add(plan(plans.size() + 1, "MONITOR", "加密检测频次",
            "跟踪处置前后指标变化，及时纠正方案",
            immediate
                ? "测温点检测频次从每日1次提升至每日3次，重点关注高温区与虫情变化"
                : "检测频次维持每周2次，重点关注高温区、水分梯度与虫情变化",
            "/pestAnalysis", "详见「虫害分析」功能"));
        return plans;
    }

    private TreatPlan plan(int priority, String primary, String title, String reason, String detail,
        String linkTo, String linkLabel) {
        TreatPlan plan = new TreatPlan();
        plan.setPriority(priority);
        plan.setPrimary(primary);
        plan.setTitle(title);
        plan.setReason(reason);
        plan.setDetail(detail);
        plan.setLinkTo(linkTo);
        plan.setLinkLabel(linkLabel);
        return plan;
    }

    private String describeDeadline(String level) {
        return "SEVERE".equals(level) ? "7天" : "15天";
    }

    private Warning findWarning(List<Warning> warnings, String type) {
        return warnings.stream().filter(w -> type.equals(w.getType())).findFirst().orElse(null);
    }

    private boolean isRisk(Warning w) {
        return w != null && ("MODERATE".equals(w.getLevel()) || "SEVERE".equals(w.getLevel()));
    }

    private String levelText(String level) {
        return switch (level) {
            case "SEVERE" -> "严重";
            case "MODERATE" -> "较重";
            case "MILD" -> "一般";
            default -> "正常";
        };
    }

    private void applyHistoryBoost(Warning w, String issueKey, GrainAnalysisReqDTO reqDTO) {
        if ("NORMAL".equals(w.getLevel()) || reqDTO.getHistoryInfo() == null
            || reqDTO.getHistoryInfo().getPastIssues() == null) {
            return;
        }
        if (reqDTO.getHistoryInfo().getPastIssues().contains(issueKey)) {
            double cap = switch (w.getType()) {
                case "TEMP_ABNORMAL" -> WEIGHT_TEMP;
                case "PEST" -> WEIGHT_PEST;
                case "MOISTURE" -> WEIGHT_MOISTURE;
                default -> WEIGHT_ENV;
            };
            w.setScore(Math.min(cap, w.getScore() * 1.15));
        }
    }

    private Warning base(String type, String name) {
        Warning w = new Warning();
        w.setType(type);
        w.setName(name);
        w.setLevel("NORMAL");
        w.setLevelText("正常");
        w.setScore(0.0);
        w.setImpact("无");
        return w;
    }

    private double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round(value * factor) / factor;
    }
}
