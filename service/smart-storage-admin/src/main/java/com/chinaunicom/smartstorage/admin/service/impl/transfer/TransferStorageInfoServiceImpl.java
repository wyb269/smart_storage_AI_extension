package com.chinaunicom.smartstorage.admin.service.impl.transfer;

import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.component.TransferStorageInfoComponent;
import com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageInfoQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.StsWarehouse;
import com.chinaunicom.smartstorage.admin.entity.transfer.TransferStorageAudit;
import com.chinaunicom.smartstorage.admin.entity.transfer.TransferStorageInfo;
import com.chinaunicom.smartstorage.admin.enumbean.TransferStorageAuditStatusEnum;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.WarehouseMapper;
import com.chinaunicom.smartstorage.admin.mapper.transfer.TransferStorageAuditMapper;
import com.chinaunicom.smartstorage.admin.mapper.transfer.TransferStorageInfoMapper;
import com.chinaunicom.smartstorage.admin.service.inter.transfer.TransferStorageInfoService;
import com.chinaunicom.smartstorage.common.enumbean.OptEnum;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 倒仓信息(TransferStorageInfo)表服务实现类
 *
 * @author liuchink
 * @since 2026-03-11 08:38:52
 */
@Slf4j
@Service
public class TransferStorageInfoServiceImpl implements TransferStorageInfoService {
    @Resource
    private TransferStorageInfoMapper transferStorageInfoMapper;
    @Resource
    private TransferStorageAuditMapper transferStorageAuditMapper;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private WarehouseMapper warehouseMapper;
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private TransferStorageInfoComponent transferStorageInfoComponent;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveTransferStorageInfo(TransferStorageInfo transferStorageInfo) {
        ActionResult<String, String> actionResult = check(transferStorageInfo, OptEnum.ADD);
        if (actionResult.getStatus().isSuccess()) {
            transferStorageInfoMapper.insert(transferStorageInfo);
            transferStorageInfoComponent.saveAuditRecord(transferStorageInfo,
                TransferStorageAuditStatusEnum.fromCode(transferStorageInfo.getStatus()));
            actionResult = actionResultService.callBackResult();
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateTransferStorageInfo(TransferStorageInfo transferStorageInfo) {
        ActionResult<String, String> actionResult = check(transferStorageInfo, OptEnum.UPDATE);
        if (actionResult.getStatus().isSuccess()) {
            transferStorageInfoMapper.updateById(transferStorageInfo);
            actionResult = actionResultService.callBackResult();
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteTransferStorageInfo(List<TransferStorageInfo> listTransferStorageInfo) {
        for (TransferStorageInfo transferStorageInfo : listTransferStorageInfo) {
            final TransferStorageInfo transferStorageInfoById =
                transferStorageInfoMapper.selectById(transferStorageInfo.getId());
            if (transferStorageInfoById != null) {
                if (transferStorageInfoById.getStatus() != TransferStorageAuditStatusEnum.PENDING.getCode()) {
                    return actionResultService.callBackResultFail("当前单据状态不允许删除");
                }
                transferStorageAuditMapper.delete(Wrappers.lambdaQuery(TransferStorageAudit.class)
                    .eq(TransferStorageAudit::getTransferId, transferStorageInfoById.getId()));
                transferStorageInfoMapper.deleteByIds(listTransferStorageInfo);
            }
        }
        return actionResultService.callBackResult();
    }

    @Override
    public List<TransferStorageInfo>

        listTransferStorageInfoByCondition(TransferStorageInfoQueryReqDTO transferStorageInfoQueryReqDTO) {
        return transferStorageInfoMapper.listTransferStorageInfoByCondition(transferStorageInfoQueryReqDTO);
    }

    @Override
    public TransferStorageInfo getTransferStorageInfoByDetail(String id) {
        return transferStorageInfoMapper.getTransferStorageInfoByDetail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> processPending(TransferStorageInfo transferStorageInfo) {
        final TransferStorageInfo transferStorageInfoById =
            transferStorageInfoMapper.selectById(transferStorageInfo.getId());
        if (transferStorageInfoById != null
            && (transferStorageInfoById.getStatus() == TransferStorageAuditStatusEnum.PENDING.getCode()
                || transferStorageInfoById.getStatus() == TransferStorageAuditStatusEnum.REJECTED.getCode())) {
            transferStorageInfo.setStatus(TransferStorageAuditStatusEnum.IN_REVIEW.getCode());
            transferStorageInfoMapper.updateById(transferStorageInfo);
            transferStorageInfoComponent.saveAuditRecord(transferStorageInfo, TransferStorageAuditStatusEnum.IN_REVIEW);
            return actionResultService.callBackResult();
        }
        return actionResultService.callBackResultFail("当前单据状态不允许提交");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> processCancel(TransferStorageInfo transferStorageInfo) {
        final TransferStorageInfo transferStorageInfoById =
            transferStorageInfoMapper.selectById(transferStorageInfo.getId());
        if (transferStorageInfoById != null
            && transferStorageInfoById.getStatus() == TransferStorageAuditStatusEnum.IN_REVIEW.getCode()) {
            transferStorageInfo.setStatus(TransferStorageAuditStatusEnum.PENDING.getCode());
            transferStorageInfoMapper.updateById(transferStorageInfo);
            transferStorageInfoComponent.saveAuditRecord(transferStorageInfo, TransferStorageAuditStatusEnum.CANCEL);
            return actionResultService.callBackResult();
        }
        return actionResultService.callBackResultFail("当前单据状态不允许撤回");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> processVoided(TransferStorageInfo transferStorageInfo) {
        final TransferStorageInfo transferStorageInfoById =
            transferStorageInfoMapper.selectById(transferStorageInfo.getId());
        if (transferStorageInfoById != null
            && transferStorageInfoById.getStatus() == TransferStorageAuditStatusEnum.REJECTED.getCode()) {
            transferStorageInfo.setStatus(TransferStorageAuditStatusEnum.VOIDED.getCode());
            transferStorageInfoMapper.updateById(transferStorageInfo);
            transferStorageInfoComponent.saveAuditRecord(transferStorageInfo, TransferStorageAuditStatusEnum.VOIDED);
            return actionResultService.callBackResult();
        }
        return actionResultService.callBackResultFail("当前单据状态不允许作废");
    }

    private ActionResult<String, String> check(TransferStorageInfo transferStorageInfo, OptEnum optEnum) {
        final ActionResult<StsWarehouse, StsWarehouse> check = transferStorageInfoComponent.check(transferStorageInfo);
        if (!check.getStatus().isSuccess()) {
            return actionResultService.callBackResultFail(check.getStatus().getCode(), check.getStatus().getMessage());
        }
        if (optEnum.equals(OptEnum.UPDATE)) {
            final TransferStorageInfo transferStorageInfoById =
                transferStorageInfoMapper.selectById(transferStorageInfo.getId());
            if (transferStorageInfoById == null
                || (transferStorageInfoById.getStatus() != TransferStorageAuditStatusEnum.PENDING.getCode()
                    && transferStorageInfoById.getStatus() != TransferStorageAuditStatusEnum.REJECTED.getCode())) {
                return actionResultService.callBackResultFail("当前单据状态不允许修改");
            }
        }
        return actionResultService.callBackResult();
    }

}
