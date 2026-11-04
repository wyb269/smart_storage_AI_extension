package com.chinaunicom.smartstorage.admin.controller.transfer;

import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.transfer.*;
import com.chinaunicom.smartstorage.admin.dto.transfer.*;
import com.chinaunicom.smartstorage.admin.service.inter.transfer.TransferStorageAuditService;
import com.chinaunicom.smartstorage.common.constant.RedisConstant;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 倒仓审核信息(TransferStorageAudit)表控制层
 *
 * @author liuchink
 * @since 2026-03-11 10:02:15
 */
@Tag(name = "倒仓审核")
@Slf4j
@RestController
@RequestMapping("/transferStorageAudit")
public class TransferStorageAuditController extends BaseController {
    /**
     * 服务对象
     */
    @Resource
    private TransferStorageAuditService transferStorageAuditService;
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private RedissonClient redissonClient;

    @Operation(summary = "审核")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/audit", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "倒仓审核", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String>
        audit(@RequestBody @Validated TransferStorageAuditReqDTO transferStorageAuditReqDTO) {
        String key = RedisConstant.TRANSFER_STORAGE_AUDIT_STATUS_KEY + transferStorageAuditReqDTO.getTransferId();
        final RLock rLock = redissonClient.getLock(key);
        try {
            final boolean lock = rLock.tryLock(3, TimeUnit.SECONDS);
            if (lock) {
                return transferStorageAuditService.audit(transferStorageAuditReqDTO);
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

    /**
     * 根据查询条件查询
     *
     * @param transferStorageAuditQueryReqDTO transferStorageInfoQueryReqDTO
     * @author liuchink
     */
    @Operation(summary = "查看审批流")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = TransferStorageAuditRespDTO.class))))
    @PostMapping(path = "/listTransferStorageAuditByCondition", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageAuditRespDTO")
    public Object listTransferStorageAuditByCondition(
        @RequestBody @Validated TransferStorageAuditQueryReqDTO transferStorageAuditQueryReqDTO) {
        return transferStorageAuditService.listTransferStorageAuditByCondition(transferStorageAuditQueryReqDTO);
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
        return transferStorageAuditService.listTransferStorageInfoByCondition(transferStorageInfoQueryReqDTO);
    }
}
