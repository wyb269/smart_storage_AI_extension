package com.chinaunicom.smartstorage.admin.service.impl.aggregation.ai;

import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation.FumigationPlanReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation.FumigationPlanRespDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.pest.PestAnalysisReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.pest.PestAnalysisRespDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.pest.PestAnalysisRespDTO.FumigationBrief;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.pest.PestAnalysisRespDTO.MonitorPlan;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.pest.PestAnalysisRespDTO.Plan;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.pest.PestAnalysisRespDTO.RiskItem;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.ai.FumigationAiService;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.ai.PestAnalysisAiService;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.ai.VentilationAiService;
import com.chinaunicom.smartstorage.admin.utils.AiAskUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI辅助 - 虫害分析算法实现
 * <p>方案A内部调用熏蒸算法、方案B调用通风判定，实现模块联动</p>
 *
 * @author smart-storage
 * @since 2026/10/21
 */
@Slf4j
@Service
public class PestAnalysisAiServiceImpl implements PestAnalysisAiService {

    /** 检疫对象虫种（危险虫粮） */
    private static final Set<String> QUARANTINE_PESTS = Set.of("KHAPRA_BEETLE", "BEAN_WEEVIL", "PEA_WEEVIL");

    /** 主要害虫编码（GB/T 29890 主要害虫口径） */
    private static final Set<String> MAIN_PESTS = Set.of("RICE_WEEVIL", "CORN_WEEVIL", "GRAIN_BORER",
        "RED_FLOUR_BEETLE", "MOTH");

    /** 虫种编码 → 中文名（风险说明文案用） */
    private static final Map<String, String> PEST_LABEL = new LinkedHashMap<>();

    static {
        PEST_LABEL.put("CORN_WEEVIL", "玉米象");
        PEST_LABEL.put("RICE_WEEVIL", "米象");
        PEST_LABEL.put("GRAIN_BORER", "谷蠹");
        PEST_LABEL.put("RED_FLOUR_BEETLE", "赤拟谷盗");
        PEST_LABEL.put("CONFUSED_FLOUR_BEETLE", "杂拟谷盗");
        PEST_LABEL.put("LONGHEADED_FLOUR_BEETLE", "长头谷盗");
        PEST_LABEL.put("FLAT_GRAIN_BEETLE", "扁谷盗");
        PEST_LABEL.put("BOOK_LOUSE", "书虱");
        PEST_LABEL.put("MOTH", "蛾类（印度谷螟）");
        PEST_LABEL.put("MITE", "螨类");
        PEST_LABEL.put("KHAPRA_BEETLE", "谷斑皮蠹");
        PEST_LABEL.put("BEAN_WEEVIL", "豆象");
        PEST_LABEL.put("PEA_WEEVIL", "豌豆象");
    }

    @Resource
    private FumigationAiService fumigationAiService;

    @Resource
    private VentilationAiService ventilationAiService;

    @Override
    public PestAnalysisRespDTO analyze(PestAnalysisReqDTO reqDTO) {
        List<String> missing = checkMissingParams(reqDTO);
        if (!missing.isEmpty()) {
            PestAnalysisRespDTO ask = new PestAnalysisRespDTO();
            ask.setRequireMoreInput(true);
            ask.setMissingParams(missing);
            ask.setAiMessage(AiAskUtil.buildAskMessage("虫害分析", missing));
            return ask;
        }
        PestAnalysisRespDTO resp = new PestAnalysisRespDTO();
        PestAnalysisReqDTO.PestInfo pest = reqDTO.getPestInfo();
        PestAnalysisReqDTO.ControlInfo control = reqDTO.getControlInfo();
        double grainTemp = reqDTO.getGrainInfo().getGrainAvgTemp();
        double maxDensity = pest.getMaxPointDensity() != null ? pest.getMaxPointDensity()
            : pest.getTotalDensity();

        // ============ 1. 虫粮等级判定（GB/T 29890 表1，双指标取高档） ============
        double total = maxDensity;
        double main = 0;
        for (PestAnalysisReqDTO.PestSpeciesItem s : pest.getSpecies()) {
            if (Boolean.TRUE.equals(s.getIsMain()) && s.getDensity() != null && s.getDensity() > main) {
                main = s.getDensity();
            }
        }
        boolean quarantine = pest.getSpecies().stream().anyMatch(s -> QUARANTINE_PESTS.contains(s.getCode()));
        String grade;
        String basis;
        String deadline;
        if (quarantine) {
            grade = "DANGEROUS";
            basis = "检出检疫对象活虫（GB/T 29890 危险虫粮）";
            deadline = "立即隔离，3天内彻底杀虫处理";
        } else if (total > 30 || main > 10) {
            grade = "SEVERE";
            basis = String.format("总密度%.0f头/kg、主要害虫%.0f头/kg，任一指标>30/>10头/kg", total, main);
            deadline = "7天内除治（GB/T 29890 严重虫粮）";
        } else if (total > 5 || main > 2) {
            grade = "GENERAL";
            basis = String.format("总密度%.0f头/kg、主要害虫%.0f头/kg，落于6~30/3~10区间", total, main);
            deadline = grainTemp >= 15 ? "15天内除治（粮温≥15℃的一般虫粮）" : "粮温≤15℃，加强检测做好防护";
        } else {
            grade = "NONE";
            basis = String.format("总密度%.0f头/kg、主要害虫%.0f头/kg，未超基本无虫粮指标", total, main);
            deadline = "加强防护，不需杀虫处理";
        }
        resp.setGrainPestGrade(grade);
        resp.setGradeBasis(basis);
        resp.setTreatDeadline(deadline);

        // ============ 2. 抗性分组与风险评分 ============
        List<String> codes = pest.getSpecies().stream().map(PestAnalysisReqDTO.PestSpeciesItem::getCode)
            .toList();
        String group = fumigationAiService.resolveResistanceGroup(codes, pest.getResistanceLevel());
        resp.setResistanceGroup(group);

        double score = gradeScore(grade, total, main, pest, reqDTO);
        resp.setRiskScore(score);
        String riskLevel = score >= 70 ? "HIGH" : score >= 40 ? "MEDIUM" : "LOW";
        resp.setRiskLevel(riskLevel);
        resp.setRiskLevelText(riskLevelText(riskLevel));

        // ============ 3. 风险项详情（原型「风险项详情」列表） ============
        List<RiskItem> riskItems = buildRiskItems(reqDTO, grade, total, grainTemp, control);
        resp.setRiskItems(riskItems);
        resp.setSummary(buildSummary(reqDTO, grade, total, riskLevel, riskItems));

        // ============ 4. 方案生成与比选 ============
        List<Plan> plans = new ArrayList<>();
        plans.add(buildPlanA(reqDTO, grade, grainTemp, group));
        plans.add(buildPlanB(reqDTO, grade, grainTemp));
        plans.add(buildPlanC(reqDTO, grade));
        // 首推规则（历史防治效果差亦首推熏蒸）
        boolean forceA = "SEVERE".equals(grade) || "DANGEROUS".equals(grade);
        boolean historyFail = control != null
            && ("INEFFECTIVE".equals(control.getEffectRating()) || "PARTIAL".equals(control.getEffectRating()))
            || reqDTO.getHistoryInfo() != null
            && (reqDTO.getHistoryInfo().getRecurrenceCount() != null
                && reqDTO.getHistoryInfo().getRecurrenceCount() >= 2
                || "BAD".equals(reqDTO.getHistoryInfo().getLastFumigationEffect()));
        for (Plan p : plans) {
            if ("A".equals(p.getCode())) {
                p.setRecommended(p.getFeasible() && (forceA || historyFail || !"NONE".equals(grade)
                    && grainTemp >= 15));
            } else if ("B".equals(p.getCode())) {
                p.setRecommended(p.getFeasible() && "NONE".equals(grade));
            } else {
                p.setRecommended(false);
            }
            p.setStarRating(starRating(p));
        }
        resp.setPlans(plans);

        // ============ 5. 防治后监测建议 ============
        MonitorPlan monitor = new MonitorPlan();
        monitor.setRecheckDays("SEVERE".equals(grade) || "DANGEROUS".equals(grade) ? 7 : 15);
        monitor.setItems(List.of("试虫笼检查（每笼≥30头活虫）", "筛检取样（4-10月每半月1次）", "诱捕器诱虫计数"));
        resp.setMonitorPlan(monitor);
        return resp;
    }

    /**
     * 参数完整性检查：缺失项一次列全，以 AI 口吻追问，不做兜底推算
     * <p>含方案A内部熏蒸联动所需的仓房体积字段</p>
     */
    private List<String> checkMissingParams(PestAnalysisReqDTO reqDTO) {
        List<String> missing = new ArrayList<>();
        PestAnalysisReqDTO.WarehouseInfo wh = reqDTO.getWarehouseInfo() != null
            ? reqDTO.getWarehouseInfo() : new PestAnalysisReqDTO.WarehouseInfo();
        if (wh.getWarehouseType() == null || wh.getWarehouseType().isBlank()) {
            missing.add("仓型");
        }
        addIfNull(missing, wh.getSpaceVolume(), "仓房空间体积");
        addIfNull(missing, wh.getGrainVolume(), "粮堆体积");
        PestAnalysisReqDTO.GrainInfo grain = reqDTO.getGrainInfo() != null
            ? reqDTO.getGrainInfo() : new PestAnalysisReqDTO.GrainInfo();
        if (grain.getGrainType() == null || grain.getGrainType().isBlank()) {
            missing.add("粮种");
        }
        addIfNull(missing, grain.getGrainMoisture(), "粮食水分");
        addIfNull(missing, grain.getGrainAvgTemp(), "平均粮温");
        addIfNull(missing, grain.getStoredDays(), "已储存天数");
        addIfNull(missing, grain.getStoredYears(), "储存年限");
        PestAnalysisReqDTO.PestInfo pest = reqDTO.getPestInfo();
        if (pest == null || pest.getSpecies() == null || pest.getSpecies().isEmpty()) {
            missing.add("虫种");
        }
        addIfNull(missing, pest == null ? null : pest.getTotalDensity(), "害虫总密度");
        return missing;
    }

    private void addIfNull(List<String> missing, Object value, String label) {
        if (value == null) {
            missing.add(label);
        }
    }

    // ==================== 风险项 ====================

    /** 风险项详情：虫口密度/环境/密封性/抗性/历史/清洁卫生/防虫设施/杂质 */
    private List<RiskItem> buildRiskItems(PestAnalysisReqDTO reqDTO, String grade, double total,
        double grainTemp, PestAnalysisReqDTO.ControlInfo control) {
        List<RiskItem> items = new ArrayList<>();
        PestAnalysisReqDTO.PestInfo pest = reqDTO.getPestInfo();
        PestAnalysisReqDTO.EnvData env = reqDTO.getEnvData();
        PestAnalysisReqDTO.GrainInfo grain = reqDTO.getGrainInfo();
        PestAnalysisReqDTO.PreventionInfo prevention = reqDTO.getPreventionInfo();
        PestAnalysisReqDTO.HistoryInfo history = reqDTO.getHistoryInfo();

        // 1. 虫口密度
        String mainSpecies = pest.getSpecies().stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsMain()))
            .map(s -> PEST_LABEL.getOrDefault(s.getCode(), s.getCode()))
            .findFirst().orElse(null);
        RiskItem density = item("PEST_DENSITY", "虫口密度严重超标",
            gradeLevel(grade), String.format("当前%.1f头/kg，%s基本无虫粮标准5头/kg%s", total,
                "NONE".equals(grade) ? "未超" : "远超",
                mainSpecies == null ? "" : String.format("，主要虫种为%s", mainSpecies)),
            "NONE".equals(grade) ? "维持常规检测" : "立即按推荐方案除治，7天内完成熏蒸处理",
            quarantineRemark(pest));
        items.add(density);

        // 2. 环境适宜性（高温高湿 + 粮温适宜）
        Double airTemp = env.getAirTemp();
        Double moisture = grain.getGrainMoisture();
        boolean hotSeason = "SUMMER".equals(env.getSeason()) || airTemp != null && airTemp >= 25;
        boolean warmGrain = grainTemp >= 20;
        boolean wetGrain = moisture != null && moisture > 13.5;
        if (hotSeason || warmGrain || wetGrain) {
            List<String> factors = new ArrayList<>();
            if (warmGrain) {
                factors.add(String.format("粮温%.1f℃", grainTemp));
            }
            if (moisture != null) {
                factors.add(String.format("水分%.1f%%", moisture));
            }
            if (hotSeason) {
                factors.add("夏季高温高湿");
            }
            items.add(item("ENVIRONMENT", "环境适宜虫害繁殖",
                hotSeason && warmGrain ? "MODERATE" : "MILD",
                String.format("%s，环境条件极利于%s等害虫繁殖", String.join(" + ", factors),
                    mainSpecies == null ? "储粮" : mainSpecies),
                "控制粮温在20℃以下、水分在安全水分以内，可显著抑制害虫繁殖", null));
        }

        // 3. 仓房密封性
        PestAnalysisReqDTO.WarehouseInfo wh = reqDTO.getWarehouseInfo();
        boolean sealingPoor = "LEVEL_3".equals(wh.getSealingLevel())
            || wh.getAirtightDecay() != null && wh.getAirtightDecay() < 90;
        items.add(item("SEALING", "仓房密封性一般",
            sealingPoor ? "MODERATE" : "MILD",
            sealingPoor
                ? "密封性不足可能导致熏蒸期间磷化氢泄漏，浓度难以维持"
                : "密封性满足熏蒸要求，熏蒸期间仍需监测浓度变化",
            "熏蒸前检查并补漏（门窗、通风口、穿墙管线），提高防治效率", "需注意"));

        // 4. 抗药性
        if ("HIGH".equals(pest.getResistanceLevel())) {
            items.add(item("RESISTANCE", "害虫抗药性偏强", "SEVERE",
                "虫种对磷化氢抗性较强，常规剂量杀虫效果下降",
                "提高设定浓度或延长密闭时间，并轮换使用硫酰氟等药剂", null));
        } else if ("MEDIUM".equals(pest.getResistanceLevel())) {
            items.add(item("RESISTANCE", "害虫抗药性一般", "MILD",
                "部分虫种对磷化氢存在一定耐受性",
                "按规程足量施药，密闭期间保证浓度不低于设定值", null));
        }

        // 5. 历史虫害复发 / 历史最高密度
        boolean recurrence = history != null && history.getRecurrenceCount() != null
            && history.getRecurrenceCount() >= 2;
        boolean hasHistory = history != null && (history.getMaxHistoricalDensity() != null
            || history.getHistoricalMainSpecies() != null);
        if (recurrence || hasHistory) {
            String desc = String.format("近一年发生%s次%s%s",
                history != null && history.getRecurrenceCount() != null ? history.getRecurrenceCount() : 0,
                history != null && history.getMaxHistoricalDensity() != null
                    ? String.format("，历史最高密度%.1f头/kg", history.getMaxHistoricalDensity()) : "",
                history != null && history.getHistoricalMainSpecies() != null
                    ? String.format("，历史主要虫种%s",
                        PEST_LABEL.getOrDefault(history.getHistoricalMainSpecies(),
                            history.getHistoricalMainSpecies())) : "");
            items.add(item("HISTORY", "虫害反复发生", recurrence ? "MODERATE" : "MILD", desc,
                recurrence ? "建议熏蒸后加强防护（防护剂/惰性粉），并缩短检测周期" : "保持常规检测频次", null));
        }

        // 6. 上次防治效果
        if (control != null && control.getEffectRating() != null) {
            boolean bad = "INEFFECTIVE".equals(control.getEffectRating())
                || "PARTIAL".equals(control.getEffectRating());
            String effect = switch (control.getEffectRating()) {
                case "EFFECTIVE" -> "有效";
                case "PARTIAL" -> "部分有效";
                default -> "无效";
            };
            String rate = control.getDensityBefore() != null && control.getDensityAfter() != null
                && control.getDensityBefore() > 0
                ? String.format("，密度由%.1f降至%.1f头/kg（防效%.0f%%）", control.getDensityBefore(),
                    control.getDensityAfter(),
                    (control.getDensityBefore() - control.getDensityAfter()) / control.getDensityBefore() * 100)
                : "";
            items.add(item("CONTROL_EFFECT", "上次防治效果" + effect, bad ? "MODERATE" : "MILD",
                String.format("上次防治方式%s，效果评价%s%s",
                    controlMethodLabel(control.getLastControlMethod()), effect, rate),
                bad ? "更换防治方式或调整药剂/剂量，避免重复无效处置" : "沿用有效防治方式并总结参数", null));
        }

        // 7. 清洁卫生与防虫设施
        if (prevention != null) {
            if ("POOR".equals(prevention.getCleaningLevel())) {
                items.add(item("CLEANLINESS", "仓房清洁卫生较差", "MILD",
                    "仓内残留粮粒、粉尘与包装物，为害虫提供滋生场所",
                    "彻底清扫仓内及周边，空仓消毒后再入粮", null));
            }
            if ("NONE".equals(prevention.getPestFacility())) {
                items.add(item("FACILITY", "防虫设施缺失", "MILD",
                    "门窗未设置防虫网/防虫线，外部害虫易侵入",
                    "补齐防虫网、防虫线并定期检查完好性", "注明"));
            }
        }

        // 8. 杂质含量
        if (grain.getImpurity() != null && grain.getImpurity() > 2.0) {
            items.add(item("IMPURITY", "杂质含量偏高", "MILD",
                String.format("杂质含量%.1f%%（>2%%），杂质区易积聚水分并成为害虫聚集点", grain.getImpurity()),
                "入仓前清理杂质，重点监测杂质区粮温与虫情", null));
        }
        return items;
    }

    private String buildSummary(PestAnalysisReqDTO reqDTO, String grade, double total,
        String riskLevel, List<RiskItem> riskItems) {
        long abnormal = riskItems.stream()
            .filter(r -> !"NORMAL".equals(r.getLevel())).count();
        String trend = "SUMMER".equals(reqDTO.getEnvData().getSeason())
            ? "，结合夏季高温高湿环境，虫口密度将继续上升，需立即采取防治措施" : "";
        return String.format("当前虫口密度%.1f头/kg%s，判定为%s，风险等级%s%s；存在%d项风险因素",
            total, "NONE".equals(grade) ? "，未超基本无虫粮标准" : "已超标", gradeLabel(grade),
            riskLevelText(riskLevel), trend, abnormal);
    }

    private RiskItem item(String type, String name, String level, String description, String suggestion,
        String remark) {
        RiskItem r = new RiskItem();
        r.setType(type);
        r.setName(name);
        r.setLevel(level);
        r.setLevelText(levelText(level));
        r.setDescription(description);
        r.setSuggestion(suggestion);
        r.setRemark(remark);
        return r;
    }

    private String quarantineRemark(PestAnalysisReqDTO.PestInfo pest) {
        boolean quarantine = pest.getSpecies().stream().anyMatch(s -> QUARANTINE_PESTS.contains(s.getCode()));
        return quarantine ? "检疫对象，需立即上报" : null;
    }

    private String gradeLevel(String grade) {
        return switch (grade) {
            case "DANGEROUS" -> "SEVERE";
            case "SEVERE" -> "SEVERE";
            case "GENERAL" -> "MODERATE";
            default -> "NORMAL";
        };
    }

    private String levelText(String level) {
        return switch (level) {
            case "SEVERE" -> "严重";
            case "MODERATE" -> "较重";
            case "MILD" -> "一般";
            default -> "正常";
        };
    }

    private String riskLevelText(String riskLevel) {
        return switch (riskLevel) {
            case "HIGH" -> "高风险";
            case "MEDIUM" -> "中风险";
            default -> "低风险";
        };
    }

    private String controlMethodLabel(String method) {
        if (method == null) {
            return "未记录";
        }
        return switch (method) {
            case "FUMIGATION" -> "熏蒸";
            case "VENTILATION" -> "通风";
            case "PHYSICAL" -> "物理防治";
            case "CHEMICAL" -> "防护剂";
            default -> method;
        };
    }

    private Integer starRating(Plan plan) {
        if (!Boolean.TRUE.equals(plan.getFeasible())) {
            return 1;
        }
        if (Boolean.TRUE.equals(plan.getRecommended())) {
            return 5;
        }
        return "C".equals(plan.getCode()) ? 2 : 3;
    }

    // ==================== 方案构建 ====================

    private Plan buildPlanA(PestAnalysisReqDTO reqDTO, String grade, double grainTemp, String group) {
        Plan p = basePlan("A", "磷化氢熏蒸 + 清洁综合防治",
            "磷化铝环流熏蒸杀灭所有虫态，配合仓房清洁消毒与防护剂防护");
        // 复用熏蒸算法生成完整方案
        FumigationPlanReqDTO fumReq = new FumigationPlanReqDTO();
        FumigationPlanReqDTO.WarehouseInfo wh = new FumigationPlanReqDTO.WarehouseInfo();
        wh.setWarehouseId(reqDTO.getWarehouseInfo().getWarehouseId());
        wh.setWarehouseName(reqDTO.getWarehouseInfo().getWarehouseName());
        wh.setWarehouseType(reqDTO.getWarehouseInfo().getWarehouseType());
        wh.setCapacity(reqDTO.getWarehouseInfo().getCapacity());
        wh.setSpaceVolume(reqDTO.getWarehouseInfo().getSpaceVolume());
        wh.setGrainVolume(reqDTO.getWarehouseInfo().getGrainVolume());
        wh.setGrainWeight(reqDTO.getWarehouseInfo().getStoredWeight());
        wh.setSealingMethod(reqDTO.getWarehouseInfo().getSealingMethod());
        wh.setAirtightLevel(reqDTO.getWarehouseInfo().getSealingLevel());
        wh.setAirtightDecay(reqDTO.getWarehouseInfo().getAirtightDecay());
        wh.setHasCirculation("CIRCULATION".equals(reqDTO.getWarehouseInfo().getSealingMethod()));
        fumReq.setWarehouseInfo(wh);
        FumigationPlanReqDTO.GrainInfo grain = new FumigationPlanReqDTO.GrainInfo();
        grain.setGrainType(reqDTO.getGrainInfo().getGrainType());
        grain.setGrainMoisture(reqDTO.getGrainInfo().getGrainMoisture());
        grain.setImpurity(reqDTO.getGrainInfo().getImpurity());
        grain.setGrainTemp(grainTemp);
        grain.setGrainAvgTemp(reqDTO.getGrainInfo().getGrainAvgTemp());
        fumReq.setGrainInfo(grain);
        FumigationPlanReqDTO.PestInfo pest = new FumigationPlanReqDTO.PestInfo();
        pest.setPestSpecies(reqDTO.getPestInfo().getSpecies().stream()
            .map(PestAnalysisReqDTO.PestSpeciesItem::getCode).toList());
        pest.setResistanceLevel(reqDTO.getPestInfo().getResistanceLevel());
        pest.setPestDensity(reqDTO.getPestInfo().getTotalDensity());
        fumReq.setPestInfo(pest);
        fumReq.setEnvInfo(null);

        FumigationPlanRespDTO fum = fumigationAiService.generatePlanCore(fumReq);
        p.setFeasible(fum.getFeasible());
        p.setFeasibilityNote(String.join("；", fum.getRejectedReasons()));
        if (Boolean.TRUE.equals(fum.getFeasible())) {
            FumigationBrief brief = new FumigationBrief();
            brief.setSchemeName(fum.getSchemeName());
            brief.setTotalDose(fum.getDose().getTotalDose());
            brief.setApplicationPoints(fum.getDose().getApplicationPoints());
            brief.setSealDays(fum.getSchedule().getSealDays());
            brief.setAerateDays(fum.getSchedule().getAerateDays());
            brief.setTargetConcentration(fum.getTargetConcentration());
            brief.setDosePerVolume(fum.getDose().getDosePerVolumePile());
            brief.setAgentName(fum.getAgentName() + (fum.getAgentSpec() == null ? "" : "(" + fum.getAgentSpec() + ")"));
            p.setFumigation(brief);
            p.setTotalDays(fum.getSchedule().getSealDays() + fum.getSchedule().getAerateDays() + 1);
        }
        p.setEffectRate(0.95);
        p.setEffectDescription("虫口密度降至0.5头/kg以下，防治有效率95%");
        p.setCostEstimate(fum.getDose() != null && fum.getDose().getTotalDose() != null
            ? round(fum.getDose().getTotalDose() * 40 + 300, 0) : null);
        p.setAdvantages(List.of("杀灭所有虫态，效果彻底", "对耐药虫种有效", "成本适中"));
        p.setDisadvantages(List.of("作业周期长", "需人员防护与安全管控", "雨天/低湿环境限制施药"));
        p.setDetailLink("/fumigation");
        return p;
    }

    private Plan buildPlanB(PestAnalysisReqDTO reqDTO, String grade, double grainTemp) {
        Plan p = basePlan("B", "低温通风抑制", "利用冬季冷源机械通风，将粮温降至15℃以下抑制害虫繁殖");
        Double airTemp = reqDTO.getEnvData().getAirTemp();
        String season = reqDTO.getEnvData().getSeason();
        boolean coolSeason = "AUTUMN".equals(season) || "WINTER".equals(season);
        boolean tempOk = airTemp != null && reqDTO.getEnvData().getAirRh() != null
            && ventilationAiService.allowCoolingAeration(
            reqDTO.getGrainInfo().getGrainType(), grainTemp,
            reqDTO.getGrainInfo().getGrainMoisture(), airTemp, reqDTO.getEnvData().getAirRh());
        if (!coolSeason) {
            p.setFeasible(false);
            p.setFeasibilityNote("当前季节无稳定冷源，低温通风抑制效果有限");
        } else if (!tempOk) {
            p.setFeasible(false);
            p.setFeasibilityNote("当前大气温度不满足降温通风条件（粮温-气温需>4℃且湿度条件达标）");
        } else {
            p.setFeasible(true);
            p.setFeasibilityNote(String.format("大气温度%.1f℃，低于粮温%.1f℃，满足降温通风条件", airTemp, grainTemp));
        }
        p.setTargetTemp(15.0);
        p.setTotalDays(25);
        p.setEffectRate(0.6);
        p.setEffectDescription("粮温降至15℃以下，抑制害虫繁殖，防治有效率60%");
        p.setCostEstimate(200.0);
        p.setAdvantages(List.of("无药剂残留", "操作简单成本低", "同时降低粮温利于储藏"));
        p.setDisadvantages(List.of("仅抑制不杀灭，虫卵可能存活", "依赖季节冷源", "气温回升期虫害易反弹"));
        p.setDetailLink("/ventilation");
        return p;
    }

    private Plan buildPlanC(PestAnalysisReqDTO reqDTO, String grade) {
        Plan p = basePlan("C", "物理防治（气调/惰性粉/诱捕）", "充氮气调、食品级惰性粉、诱捕器监测等非药剂手段");
        Double decay = reqDTO.getWarehouseInfo().getAirtightDecay();
        boolean levelOk = "LEVEL_1".equals(reqDTO.getWarehouseInfo().getSealingLevel())
            || "LEVEL_2".equals(reqDTO.getWarehouseInfo().getSealingLevel());
        boolean airtightOk = decay != null && decay >= 120 || decay == null && levelOk;
        if (!"NONE".equals(grade)) {
            p.setFeasible(false);
            p.setFeasibilityNote("虫粮等级已达" + gradeLabel(grade) + "，物理防治见效慢，建议以熏蒸为主");
        } else if (!airtightOk) {
            p.setFeasible(false);
            p.setFeasibilityNote("仓房气密性未达气调要求（压力半衰期≥120s）");
        } else {
            p.setFeasible(true);
            p.setFeasibilityNote("气密性达标，可实施充氮气调或惰性粉防护");
        }
        p.setTotalDays(30);
        p.setEffectRate(0.5);
        p.setEffectDescription("物理防治，防治有效率50%，适合作为预防与长期防护手段");
        p.setCostEstimate(800.0);
        p.setAdvantages(List.of("绿色无残留", "适合有机粮/种子粮", "可长期防护"));
        p.setDisadvantages(List.of("见效慢", "对仓房气密性要求高", "虫害密度高时效果差"));
        p.setDetailLink(null);
        return p;
    }

    private Plan basePlan(String code, String name, String summary) {
        Plan p = new Plan();
        p.setCode(code);
        p.setName(name);
        p.setSummary(summary);
        p.setRecommended(false);
        return p;
    }

    // ==================== 评分 ====================

    private double gradeScore(String grade, double total, double main, PestAnalysisReqDTO.PestInfo pest,
        PestAnalysisReqDTO reqDTO) {
        double score;
        switch (grade) {
            case "DANGEROUS":
                score = 30;
                break;
            case "SEVERE":
                score = 30;
                break;
            case "GENERAL":
                score = 15;
                break;
            default:
                score = 0;
        }
        // 全面分布 +5
        boolean whole = pest.getSpecies().stream()
            .anyMatch(s -> "WHOLE".equals(s.getDistribution()));
        if (whole) {
            score = Math.min(30, score + 5);
        }
        // 近一年复发≥2次 +5
        if (reqDTO.getHistoryInfo() != null && reqDTO.getHistoryInfo().getRecurrenceCount() != null
            && reqDTO.getHistoryInfo().getRecurrenceCount() >= 2) {
            score = Math.min(30, score + 5);
        }
        // 防护剂在用 -5
        if (reqDTO.getPreventionInfo() != null
            && Boolean.TRUE.equals(reqDTO.getPreventionInfo().getProtectantUsed())) {
            score = Math.max(0, score - 5);
        }
        return score * (100.0 / 30.0) * 0.78 + (score > 0 ? 22 : 0);
    }

    private String gradeLabel(String grade) {
        switch (grade) {
            case "GENERAL":
                return "一般虫粮";
            case "SEVERE":
                return "严重虫粮";
            case "DANGEROUS":
                return "危险虫粮";
            default:
                return "基本无虫粮";
        }
    }

    private double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round(value * factor) / factor;
    }
}
