package com.chinaunicom.smartstorage.admin.controller.card;

import cn.hutool.core.convert.Convert;
import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.dto.card.*;
import com.google.common.collect.Lists;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.card.*;
import com.chinaunicom.smartstorage.admin.entity.card.SmartCard;
import com.chinaunicom.smartstorage.admin.service.inter.card.SmartCardService;
import com.chinaunicom.smartstorage.common.bean.BatchIdReqDTO;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.optlog.OptLog;
import com.chinaunicom.smartstorage.common.optlog.OptLogOperatorType;
import com.chinaunicom.smartstorage.common.page.QueryByPage;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * (SmartCard)表控制层
 *
 * @author liuchink
 * @since 2026-01-22 13:35:41
 */
@Tag(name = "智能卡管理")
@Slf4j
@RestController
@RequestMapping("/smartCard")
public class SmartCardController extends BaseController {
    /**
     * 服务对象
     */
    @Resource
    private SmartCardService smartCardService;
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private JwtTokenComponent jwtTokenComponent;

    /**
     * 新增数据
     *
     * @param smartCardSaveReqDTO smartCardSaveReqDTO
     * @return 新增结果
     * @author liuchink
     */
    @Operation(summary = "添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/saveSmartCard", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "智能卡管理", operatorType = OptLogOperatorType.INSERT)
    public ActionResult<String, String> saveSmartCard(@RequestBody @Validated SmartCardSaveReqDTO smartCardSaveReqDTO) {
        SmartCard smartCard = mapperFacade.map(smartCardSaveReqDTO, SmartCard.class);
        smartCard.setId(Convert.toStr(uidGenerator.getUID()));
        smartCard.setAddTime(LocalDateTime.now());
        smartCard.setAddUser(jwtTokenComponent.getUser().getId());
        smartCard.setState(2);
        return smartCardService.saveSmartCard(smartCard);
    }

    /**
     * 修改数据
     *
     * @param smartCardUpdateReqDTO smartCardUpdateReqDTO
     * @return 修改结果
     * @author liuchink
     */
    @Operation(summary = "修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/updateSmartCard", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "智能卡管理", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String>
        updateSmartCard(@RequestBody @Validated SmartCardUpdateReqDTO smartCardUpdateReqDTO) {
        SmartCard smartCard = mapperFacade.map(smartCardUpdateReqDTO, SmartCard.class);
        return smartCardService.updateSmartCard(smartCard);
    }

    /**
     * 删除数据
     *
     * @param batchIdReqDTO batchIdReqDTO
     * @return 删除结果
     * @author liuchink
     */
    @Operation(summary = "删除")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/deleteSmartCard", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "智能卡管理", operatorType = OptLogOperatorType.DELETE)
    public ActionResult<String, String> deleteSmartCard(@RequestBody @Validated BatchIdReqDTO batchIdReqDTO) {
        List<SmartCard> listSmartCard = Lists.newArrayList();
        for (String id : batchIdReqDTO.getIds()) {
            SmartCard smartCard = new SmartCard();
            smartCard.setId(id);
            listSmartCard.add(smartCard);
        }
        return smartCardService.deleteSmartCard(listSmartCard);
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id id
     * @return 单条数据
     * @author liuchink
     */
    @Operation(summary = "根据ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getSmartCardById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<SmartCardRespDTO, String> getSmartCardById(@PathVariable String id) {
        SmartCard smartCardById = smartCardService.getSmartCardById(id);
        SmartCardRespDTO smartCardRespDTO = mapperFacade.map(smartCardById, SmartCardRespDTO.class);
        return actionResultService.callBackResult(smartCardRespDTO);
    }

    @Operation(summary = "入库读卡")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/inSwipeCard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<SwipeCardRespDTO, String> inSwipeCard(@RequestBody @Validated SwipeCardReqDTO swipeCardReqDTO) {
        return smartCardService.inSwipeCard(swipeCardReqDTO);
    }

    @Operation(summary = "出库读卡")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/outSwipeCard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<SwipeCardRespDTO, String>
        outSwipeCard(@RequestBody @Validated SwipeCardReqDTO swipeCardReqDTO) {
        return smartCardService.outSwipeCard(swipeCardReqDTO);
    }

    /**
     * 根据查询条件查询
     *
     * @param smartCardQueryReqDTO smartCardQueryReqDTO
     * @author liuchink
     */
    @Operation(summary = "根据条件查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = SmartCardRespDTO.class))))
    @PostMapping(path = "/listSmartCardByCondition", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.card.SmartCardRespDTO")
    public Object listSmartCardByCondition(@RequestBody @Validated SmartCardQueryReqDTO smartCardQueryReqDTO) {
        return smartCardService.listSmartCardByCondition(smartCardQueryReqDTO);
    }
}
