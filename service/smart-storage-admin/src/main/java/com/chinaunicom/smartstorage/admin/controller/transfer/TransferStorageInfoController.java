package com.chinaunicom.smartstorage.admin.controller.transfer;

import cn.hutool.core.convert.Convert;
import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageInfoQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageInfoSaveReqDTO;
import com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageInfoUpdateReqDTO;
import com.chinaunicom.smartstorage.admin.entity.transfer.TransferStorageInfo;
import com.chinaunicom.smartstorage.admin.enumbean.TransferStorageAuditStatusEnum;
import com.chinaunicom.smartstorage.admin.service.inter.transfer.TransferStorageInfoService;
import com.chinaunicom.smartstorage.common.bean.BatchIdReqDTO;
import com.chinaunicom.smartstorage.common.constant.RedisConstant;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.optlog.OptLog;
import com.chinaunicom.smartstorage.common.optlog.OptLogOperatorType;
import com.chinaunicom.smartstorage.common.page.QueryByPage;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 倒仓信息(TransferStorageInfo)表控制层
 *
 * @author liuchink
 * @since 2026-03-11 08:38:52
 */
@Tag(name = "倒仓信息")
@Slf4j
@RestController
@RequestMapping("/transferStorageInfo")
public class TransferStorageInfoController extends BaseController {
    /**
     * 服务对象
     */
    @Resource
    private TransferStorageInfoService transferStorageInfoService;
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private RedissonClient redissonClient;

    /**
     * 新增数据
     *
     * @param transferStorageInfoSaveReqDTO transferStorageInfoSaveReqDTO
     * @return 新增结果
     * @author liuchink
     */
    @Operation(summary = "添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/saveTransferStorageInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "倒仓信息", operatorType = OptLogOperatorType.INSERT)
    public ActionResult<String, String>
        saveTransferStorageInfo(@RequestBody @Validated TransferStorageInfoSaveReqDTO transferStorageInfoSaveReqDTO) {
        TransferStorageInfo transferStorageInfo =
            mapperFacade.map(transferStorageInfoSaveReqDTO, TransferStorageInfo.class);
        transferStorageInfo.setId(Convert.toStr(uidGenerator.getUID()));
        transferStorageInfo.setAddUser(jwtTokenComponent.getUser().getId());
        transferStorageInfo.setAddTime(LocalDateTime.now());
        transferStorageInfo.setStatus(TransferStorageAuditStatusEnum.PENDING.getCode());
        return transferStorageInfoService.saveTransferStorageInfo(transferStorageInfo);
    }

    /**
     * 修改数据
     *
     * @param transferStorageInfoUpdateReqDTO transferStorageInfoUpdateReqDTO
     * @return 修改结果
     * @author liuchink
     */
    @Operation(summary = "修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/updateTransferStorageInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "倒仓信息", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String> updateTransferStorageInfo(
        @RequestBody @Validated TransferStorageInfoUpdateReqDTO transferStorageInfoUpdateReqDTO) {
        String transferStorageAuditStatusKey =
            RedisConstant.TRANSFER_STORAGE_AUDIT_STATUS_KEY + transferStorageInfoUpdateReqDTO.getId();
        final RLock rLock = redissonClient.getLock(transferStorageAuditStatusKey);
        try {
            final boolean lock = rLock.tryLock(3, TimeUnit.SECONDS);
            if (lock) {
                TransferStorageInfo transferStorageInfo =
                    mapperFacade.map(transferStorageInfoUpdateReqDTO, TransferStorageInfo.class);
                transferStorageInfo.setUpdateUser(jwtTokenComponent.getUser().getId());
                transferStorageInfo.setUpdateTime(LocalDateTime.now());
                return transferStorageInfoService.updateTransferStorageInfo(transferStorageInfo);
            } else {
                return actionResultService.callBackResultFail("修改数据超时，请稍后重试");
            }

        } catch (InterruptedException interruptedException) {
            log.error(SystemConstant.LOG_ERROR_TITLE, interruptedException.getMessage(), interruptedException);
            Thread.currentThread().interrupt();
            throw new CustomException();
        } finally {
            if (rLock.isLocked() && rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }

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
    @PostMapping(path = "/deleteTransferStorageInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "倒仓信息", operatorType = OptLogOperatorType.DELETE)
    public ActionResult<String, String> deleteTransferStorageInfo(@RequestBody @Validated BatchIdReqDTO batchIdReqDTO) {
        List<TransferStorageInfo> listTransferStorageInfo = Lists.newArrayList();
        for (String id : batchIdReqDTO.getIds()) {
            TransferStorageInfo transferStorageInfo = new TransferStorageInfo();
            transferStorageInfo.setId(id);
            listTransferStorageInfo.add(transferStorageInfo);
        }
        return transferStorageInfoService.deleteTransferStorageInfo(listTransferStorageInfo);
    }

    @Operation(summary = "查看详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getTransferStorageInfoByDetail/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<TransferStorageInfoRespDTO, String> getTransferStorageInfoByDetail(@PathVariable String id) {
        TransferStorageInfo transferStorageInfoByDetail = transferStorageInfoService.getTransferStorageInfoByDetail(id);
        TransferStorageInfoRespDTO transferStorageInfoRespDTO =
            mapperFacade.map(transferStorageInfoByDetail, TransferStorageInfoRespDTO.class);
        return actionResultService.callBackResult(transferStorageInfoRespDTO);
    }

    @Operation(summary = "提交")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/processPending/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "倒仓信息", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String> processPending(@PathVariable String id) {
        return executeWithLock(id, transferStorageInfoService::processPending);
    }

    @Operation(summary = "撤回")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/processCancel/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "倒仓信息", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String> processCancel(@PathVariable String id) {
        return executeWithLock(id, transferStorageInfoService::processCancel);
    }

    @Operation(summary = "作废")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/processVoided/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "倒仓信息", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String> processVoided(@PathVariable String id) {
        return executeWithLock(id, transferStorageInfoService::processVoided);
    }

    /**
     * 根据查询条件查询
     *
     * @param transferStorageInfoQueryReqDTO transferStorageInfoQueryReqDTO
     * @author liuchink
     */
    @Operation(summary = "根据条件查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = TransferStorageInfoRespDTO.class))))
    @PostMapping(path = "/listTransferStorageInfoByCondition", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageInfoRespDTO")
    public Object listTransferStorageInfoByCondition(
        @RequestBody @Validated TransferStorageInfoQueryReqDTO transferStorageInfoQueryReqDTO) {
        transferStorageInfoQueryReqDTO.setUserId(jwtTokenComponent.getUser().getId());
        return transferStorageInfoService.listTransferStorageInfoByCondition(transferStorageInfoQueryReqDTO);
    }

    /**
     * 带分布式锁的执行模板方法
     *
     * @param id 业务 ID
     * @param operation 具体的业务操作
     * @return 执行结果
     */
    private ActionResult<String, String> executeWithLock(String id,
        Function<TransferStorageInfo, ActionResult<String, String>> operation) {
        String key = RedisConstant.TRANSFER_STORAGE_AUDIT_STATUS_KEY + id;
        final RLock rLock = redissonClient.getLock(key);
        try {
            final boolean lock = rLock.tryLock(3, TimeUnit.SECONDS);
            if (lock) {
                TransferStorageInfo transferStorageInfo = new TransferStorageInfo();
                transferStorageInfo.setId(id);
                return operation.apply(transferStorageInfo);
            } else {
                return actionResultService.callBackResultFail("状态修改超时，请稍后重试");
            }

        } catch (InterruptedException interruptedException) {
            log.error(SystemConstant.LOG_ERROR_TITLE, interruptedException.getMessage(), interruptedException);
            Thread.currentThread().interrupt();
            throw new CustomException();
        } finally {
            if (rLock.isLocked() && rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }
    }
}
