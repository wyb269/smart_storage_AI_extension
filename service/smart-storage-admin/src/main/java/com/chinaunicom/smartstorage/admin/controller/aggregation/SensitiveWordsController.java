package com.chinaunicom.smartstorage.admin.controller.aggregation;

import cn.hutool.core.convert.Convert;
import com.baidu.fsg.uid.UidGenerator;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.component.SensitiveWordsComponent;
import com.chinaunicom.smartstorage.admin.dto.aggregation.sensitive.SensitiveWordsQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.sensitive.SensitiveWordsRespDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.sensitive.SensitiveWordsSaveReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.sensitive.SensitiveWordsUpdateReqDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.SensitiveWords;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.SensitiveWordsService;
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
import org.apache.commons.compress.utils.Lists;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author liuchink
 * @since 2020/4/5 15:19
 */
@Tag(name = "系统管理-敏感词")
@RestController
@RequestMapping("/aggregation/sensitive")
@Slf4j
public class SensitiveWordsController extends BaseController {
    @Resource
    private SensitiveWordsService sensitiveWordsService;
    @Resource
    private SensitiveWordBs sensitiveWordBs;
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private SensitiveWordsComponent sensitiveWordsComponent;

    /**
     * 添加
     *
     * @param sensitiveWordsSaveReqDTO organizationReqDTO
     * @author liuchink
     * @since 2018/8/21 16:30
     */
    @Operation(summary = "添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/saveSensitiveWords", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "敏感词管理", operatorType = OptLogOperatorType.INSERT)
    public ActionResult<String, String> saveSensitiveWords(
        @RequestBody @Validated SensitiveWordsSaveReqDTO sensitiveWordsSaveReqDTO) {
        SensitiveWords sensitiveWords = mapperFacade.map(sensitiveWordsSaveReqDTO, SensitiveWords.class);
        sensitiveWords.setId(Convert.toStr(uidGenerator.getUID()));
        sensitiveWords.setAddUser(jwtTokenComponent.getUser().getId());
        sensitiveWords.setAddTime(LocalDateTime.now());
        final ActionResult<String, String> actionResult = sensitiveWordsService.saveSensitiveWords(sensitiveWords);
        sensitiveWordsComponent.refresh();
        return actionResult;
    }

    /**
     * 修改
     *
     * @param sensitiveWordsUpdateReqDTO OrganizationUpdateReqDTO
     * @author liuchink
     * @since 2018/8/21 16:30
     */
    @Operation(summary = "修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "敏感词管理", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/updateSensitiveWords", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> updateSensitiveWords(
        @RequestBody @Validated SensitiveWordsUpdateReqDTO sensitiveWordsUpdateReqDTO) {
        SensitiveWords sensitiveWords = mapperFacade.map(sensitiveWordsUpdateReqDTO, SensitiveWords.class);
        sensitiveWords.setUpdateUser(jwtTokenComponent.getUser().getId());
        sensitiveWords.setUpdateTime(LocalDateTime.now());
        final ActionResult<String, String> actionResult = sensitiveWordsService.updateSensitiveWords(sensitiveWords);
        sensitiveWordsComponent.refresh();
        return actionResult;
    }

    /**
     * 删除
     *
     * @author liuchink
     * @since 2018/8/21 16:31
     */
    @Operation(summary = "删除")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "敏感词管理", operatorType = OptLogOperatorType.DELETE)
    @PostMapping(path = "/deleteSensitiveWords", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> deleteSensitiveWords(@RequestBody @Validated BatchIdReqDTO batchIdReqDTO) {
        List<SensitiveWords> listSensitiveWords = Lists.newArrayList();
        for (String id : batchIdReqDTO.getIds()) {
            SensitiveWords sensitiveWords = new SensitiveWords();
            sensitiveWords.setId(id);
            listSensitiveWords.add(sensitiveWords);
        }
        final ActionResult<String, String> actionResult =
            sensitiveWordsService.deleteSensitiveWords(listSensitiveWords);
        sensitiveWordsComponent.refresh();
        return actionResult;
    }

    /**
     * 根据组织机构ID查询机构信息
     *
     * @param id id
     * @author liuchink
     * @since 2018/8/21 16:17
     */
    @Operation(summary = "根据ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getSensitiveWordsById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<SensitiveWordsRespDTO, String> getSensitiveWordsById(
         @PathVariable String id) {
        SensitiveWords sensitiveWordsById = sensitiveWordsService.getSensitiveWordsById(id);
        SensitiveWordsRespDTO sensitiveWordsRespDTO = mapperFacade.map(sensitiveWordsById, SensitiveWordsRespDTO.class);
        return actionResultService.callBackResult(sensitiveWordsRespDTO);
    }

    /**
     * 根据查询条件查询
     *
     * @param sensitiveWordsQueryReqDTO OrganizationQueryReqDTO
     * @author liuchink
     * @since 2018/8/21 16:31
     */
    @Operation(summary = "根据条件查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = SensitiveWordsRespDTO.class))))
    @PostMapping(path = "/listSensitiveWordsByConditionOnPage", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.aggregation.sensitive.SensitiveWordsRespDTO")
    public Object listSensitiveWordsByConditionOnPage(
        @RequestBody @Validated SensitiveWordsQueryReqDTO sensitiveWordsQueryReqDTO) {
        return sensitiveWordsService.listSensitiveWordsByConditionOnPage(sensitiveWordsQueryReqDTO);
    }

}
