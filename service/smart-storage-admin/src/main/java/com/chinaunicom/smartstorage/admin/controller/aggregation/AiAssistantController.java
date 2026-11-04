package com.chinaunicom.smartstorage.admin.controller.aggregation;

import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation.FumigationPlanReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.fumigation.FumigationPlanRespDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.grain.GrainAnalysisReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.grain.GrainAnalysisRespDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.pest.PestAnalysisReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.pest.PestAnalysisRespDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.ventilation.VentilationPlanReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ai.ventilation.VentilationPlanRespDTO;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.ai.FumigationAiService;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.ai.GrainAnalysisAiService;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.ai.PestAnalysisAiService;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.ai.VentilationAiService;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI智能辅助（粮情智能分析/虫害分析/通风辅助/熏蒸辅助）
 *
 * @author smart-storage
 * @since 2026/10/21
 */
@Tag(name = "AI智能辅助")
@Slf4j
@Validated
@RestController
@RequestMapping("/ai")
public class AiAssistantController extends BaseController {

    @Resource
    private VentilationAiService ventilationAiService;

    @Resource
    private FumigationAiService fumigationAiService;

    @Resource
    private PestAnalysisAiService pestAnalysisAiService;

    @Resource
    private GrainAnalysisAiService grainAnalysisAiService;

    @Operation(summary = "通风辅助 - 生成通风方案")
    @PostMapping(path = "/ventilation/generatePlan", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<VentilationPlanRespDTO, String> generateVentilationPlan(
        @RequestBody @Valid VentilationPlanReqDTO reqDTO) {
        return actionResultService.callBackResult("生成成功", ventilationAiService.generatePlan(reqDTO), null);
    }

    @Operation(summary = "熏蒸辅助 - 生成熏蒸方案")
    @PostMapping(path = "/fumigation/generatePlan", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<FumigationPlanRespDTO, String> generateFumigationPlan(
        @RequestBody @Valid FumigationPlanReqDTO reqDTO) {
        return actionResultService.callBackResult("生成成功", fumigationAiService.generatePlan(reqDTO), null);
    }

    @Operation(summary = "虫害分析")
    @PostMapping(path = "/pestAnalysis/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<PestAnalysisRespDTO, String> analyzePest(
        @RequestBody @Valid PestAnalysisReqDTO reqDTO) {
        return actionResultService.callBackResult("分析完成", pestAnalysisAiService.analyze(reqDTO), null);
    }

    @Operation(summary = "粮情分析")
    @PostMapping(path = "/grainAnalysis/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<GrainAnalysisRespDTO, String> analyzeGrain(
        @RequestBody @Valid GrainAnalysisReqDTO reqDTO) {
        return actionResultService.callBackResult("分析完成", grainAnalysisAiService.analyze(reqDTO), null);
    }
}
