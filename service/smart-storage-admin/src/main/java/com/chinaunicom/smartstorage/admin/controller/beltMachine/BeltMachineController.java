package com.chinaunicom.smartstorage.admin.controller.beltMachine;

import com.chinaunicom.smartstorage.admin.constant.BeltMachineConstant;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventDetailRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineExportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineMonitorPointRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineRecentEventReqDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineStreamInfoReqDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineStreamInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.algorithm.BeltMachineAlgorithmRerunRespDTO;
import com.chinaunicom.smartstorage.admin.dto.beltMachine.algorithm.BeltMachineAlgorithmTaskRespDTO;
import com.chinaunicom.smartstorage.admin.service.inter.beltMachine.BeltMachineService;
import com.chinaunicom.smartstorage.admin.utils.UniversalExcelUtil;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.optlog.OptLog;
import com.chinaunicom.smartstorage.common.optlog.OptLogOperatorType;
import com.chinaunicom.smartstorage.common.page.QueryByPage;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 皮带机下站人监测管理端接口。
 *
 * <p>全部使用 POST，沿用项目既有风格。分页方法由 {@link QueryByPage} 切面
 * 自动包装为 {@code ActionResult<QueryPageResult<T>, String>}，
 * 前端取 {@code result.data} 与 {@code result.total}。</p>
 *
 * <p><b>安全约束</b>：本层不做模型推理，不返回摄像头凭据，
 * 不信任前端传入的处理人身份。</p>
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
@Slf4j
@Tag(name = "皮带机下站人监测")
@RestController
@RequestMapping("/beltMachine")
public class BeltMachineController extends BaseController {

    @Resource
    private BeltMachineService beltMachineService;

    /**
     * 查询当前用户可见的皮带机监测点位状态。
     */
    @Operation(summary = "查询皮带机监测点位状态列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = BeltMachineMonitorPointRespDTO.class))))
    @PostMapping(path = "/listMonitorPoint", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + BeltMachineConstant.Permission.FEATURE
            + "','" + BeltMachineConstant.Permission.VIEW + "')")
    public ActionResult<List<BeltMachineMonitorPointRespDTO>, String> listMonitorPoint() {
        return actionResultService.callBackResult(beltMachineService.listMonitorPoint());
    }

    /**
     * 获取指定皮带机点位绑定摄像头的短时播放信息。
     *
     * <p>地址为短时签名，过期后前端需重新调用。</p>
     */
    @Operation(summary = "获取视频播放信息")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = BeltMachineStreamInfoRespDTO.class))))
    @PostMapping(path = "/getStreamInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + BeltMachineConstant.Permission.FEATURE
            + "','" + BeltMachineConstant.Permission.VIEW + "')")
    public ActionResult<BeltMachineStreamInfoRespDTO, String> getStreamInfo(
            @RequestBody(required = false) @Valid BeltMachineStreamInfoReqDTO streamInfoReqDTO) {
        // 请求体缺省时给出可理解的业务错误，而不是 NPE
        if (streamInfoReqDTO == null || streamInfoReqDTO.getPointId() == null
                || streamInfoReqDTO.getPointId().isBlank()) {
            return actionResultService.callBackResultFail("监测点位标识不能为空");
        }
        return beltMachineService.getStreamInfo(streamInfoReqDTO.getPointId());
    }

    /**
     * 查询首页最近事件。
     */
    @Operation(summary = "查询最近皮带机下站人事件")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = BeltMachineEventRespDTO.class))))
    @PostMapping(path = "/listRecentEvent", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + BeltMachineConstant.Permission.FEATURE
            + "','" + BeltMachineConstant.Permission.VIEW + "')")
    public ActionResult<List<BeltMachineEventRespDTO>, String> listRecentEvent(
            @RequestBody(required = false) @Valid BeltMachineRecentEventReqDTO recentEventReqDTO) {
        int limit = recentEventReqDTO == null || recentEventReqDTO.getLimit() == null
                ? BeltMachineConstant.DEFAULT_RECENT_EVENT_LIMIT
                : recentEventReqDTO.getLimit();
        return actionResultService.callBackResult(beltMachineService.listRecentEvent(limit));
    }

    /**
     * 历史记录服务端分页查询，默认按发生时间倒序。
     */
    @Operation(summary = "皮带机闲人记录分页查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = BeltMachineEventRespDTO.class))))
    @PostMapping(path = "/listEventByCondition", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.beltMachine.BeltMachineEventRespDTO")
    @PreAuthorize("@aiVisionPermission.has('" + BeltMachineConstant.Permission.FEATURE
            + "','" + BeltMachineConstant.Permission.VIEW + "')")
    public Object listEventByCondition(@RequestBody @Validated BeltMachineEventQueryReqDTO query) {
        return beltMachineService.listEventByCondition(query);
    }

    /**
     * 查询事件详情、证据与处理记录。
     */
    @Operation(summary = "查询皮带机下站人事件详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = BeltMachineEventDetailRespDTO.class))))
    @PostMapping(path = "/getEventDetail/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + BeltMachineConstant.Permission.FEATURE
            + "','" + BeltMachineConstant.Permission.VIEW + "')")
    public ActionResult<BeltMachineEventDetailRespDTO, String> getEventDetail(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(beltMachineService.getEventDetail(eventId));
    }

    /**
     * 查看事件后自动标记为已处理。
     *
     * <p>前端在打开事件详情后调用本接口。仅当事件处于未核实（PENDING）时写入，
     * 重复调用无副作用。处理人由后端按登录态记录。</p>
     *
     * <p>刻意不接受前端传入的处理人、状态与意见，参数只有事件标识。</p>
     */
    @Operation(summary = "查看后标记为已处理")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "皮带机下站人事件查看标记", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/markEventViewed/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + BeltMachineConstant.Permission.FEATURE
            + "','" + BeltMachineConstant.Permission.VIEW + "')")
    public ActionResult<String, String> markEventViewed(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return beltMachineService.markEventViewed(eventId);
    }

    /**
     * 导出当前查询条件下的皮带机闲人记录。
     *
     * <p>与项目既有 ExcelExportController 一致，采用同步直出方案。</p>
     *
     * <p><b>模板 key 必须用 {@link BeltMachineConstant#EXPORT_BIZ_TYPE}，
     * 不能用 {@code exportReqDTO.getSource()}。</b>{@code source} 表示发起导出的页面
     * （前端传 HISTORY / RECENT），而 {@code ExcelExportConfig.config} 的 key 是报表模板标识；
     * 把 source 当模板 key 会得到 null 然后 NPE。</p>
     */
    @Operation(summary = "导出皮带机闲人记录")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "皮带机闲人记录导出", operatorType = OptLogOperatorType.EXPORT)
    @PostMapping(path = "/exportEvent")
    @PreAuthorize("@aiVisionPermission.has('" + BeltMachineConstant.Permission.FEATURE
            + "','" + BeltMachineConstant.Permission.EXPORT + "')")
    public void exportEvent(@RequestBody @Valid BeltMachineExportReqDTO exportReqDTO,
                            HttpServletResponse response) throws Exception {
        List<?> data = beltMachineService.listEventByConditionForExport(exportReqDTO);
        UniversalExcelUtil.export(response, data, BeltMachineConstant.EXPORT_BIZ_TYPE);
    }

    /* ==================== 算法任务（重新识别链路） ==================== */

    /**
     * 对事件发起「重新识别」。
     *
     * <p><b>权限取舍</b>：使用 VIEW 而不是 PROCESS。重跑本质是<b>重新取证</b>，
     * 只写事件的算法列，不产生也不改写任何人工结论；本功能 PROCESS 键
     * 尚未在权限配置中开放（产品口径为「查看即标记」），若把重跑挂在 PROCESS 下，
     * 该按钮将永远 403。写入的数据恒为模拟结果并带 is_simulated 标识。</p>
     *
     * <p>不在本请求中等待算法：受理后立即返回任务标识，
     * 前端轮询 {@code /algorithmTask/{taskId}}（AI_HANDOFF 第 24.1 节）。</p>
     */
    @Operation(summary = "发起皮带机事件重新识别")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = BeltMachineAlgorithmRerunRespDTO.class))))
    @OptLog(title = "皮带机事件重新识别", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/rerunAlgorithm/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + BeltMachineConstant.Permission.FEATURE
            + "','" + BeltMachineConstant.Permission.VIEW + "')")
    public ActionResult<BeltMachineAlgorithmRerunRespDTO, String> rerunAlgorithm(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(beltMachineService.rerunAlgorithm(eventId));
    }

    /**
     * 查询算法任务最新状态（轮询接口）。
     *
     * <p>未进终态时向网关取最新进度并落库；任务成功时回写事件的
     * algorithm_task_id / algorithm_status / algorithm_result / confidence /
     * conclusion_type，<b>不触碰 process_status</b>。</p>
     */
    @Operation(summary = "查询皮带机算法任务")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = BeltMachineAlgorithmTaskRespDTO.class))))
    @PostMapping(path = "/algorithmTask/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + BeltMachineConstant.Permission.FEATURE
            + "','" + BeltMachineConstant.Permission.VIEW + "')")
    public ActionResult<BeltMachineAlgorithmTaskRespDTO, String> getAlgorithmTask(
            @Parameter(description = "算法任务标识（受理接口返回的 taskId）")
            @NotBlank(message = "算法任务标识不能为空") @PathVariable String taskId) {
        return actionResultService.callBackResult(beltMachineService.getAlgorithmTask(taskId));
    }

    /**
     * 查询事件的算法任务列表，按开始时间倒序（最多 20 条）。
     *
     * <p>详情弹窗首屏数据已随 {@code getEventDetail} 返回，
     * 本接口供「算法任务」独立刷新场景使用。</p>
     */
    @Operation(summary = "查询皮带机事件算法任务列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = BeltMachineAlgorithmTaskRespDTO.class))))
    @PostMapping(path = "/listAlgorithmTasks/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@aiVisionPermission.has('" + BeltMachineConstant.Permission.FEATURE
            + "','" + BeltMachineConstant.Permission.VIEW + "')")
    public ActionResult<List<BeltMachineAlgorithmTaskRespDTO>, String> listAlgorithmTasks(
            @Parameter(description = "事件标识")
            @NotBlank(message = "事件标识不能为空") @PathVariable String eventId) {
        return actionResultService.callBackResult(beltMachineService.listAlgorithmTasks(eventId));
    }
}
