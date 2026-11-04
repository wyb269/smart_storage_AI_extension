package com.chinaunicom.smartstorage.admin.service.impl.transfer;

import com.chinaunicom.smartstorage.admin.component.TransferStorageInfoComponent;
import com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageAuditQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageAuditReqDTO;
import com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageInfoQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.StsWarehouse;
import com.chinaunicom.smartstorage.admin.entity.transfer.TransferStorageAudit;
import com.chinaunicom.smartstorage.admin.entity.transfer.TransferStorageInfo;
import com.chinaunicom.smartstorage.admin.enumbean.TransferStorageAuditStatusEnum;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.WarehouseMapper;
import com.chinaunicom.smartstorage.admin.mapper.transfer.TransferStorageAuditMapper;
import com.chinaunicom.smartstorage.admin.mapper.transfer.TransferStorageInfoMapper;
import com.chinaunicom.smartstorage.admin.service.inter.transfer.TransferStorageAuditService;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 倒仓审核信息(TransferStorageAudit)表服务实现类
 *
 * @author liuchink
 * @since 2026-03-11 10:02:15
 */
@Slf4j
@Service
public class TransferStorageAuditServiceImpl implements TransferStorageAuditService {
    @Resource
    private TransferStorageAuditMapper transferStorageAuditMapper;
    @Resource
    private TransferStorageInfoMapper transferStorageInfoMapper;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private TransferStorageInfoComponent transferStorageInfoComponent;
    @Resource
    private WarehouseMapper warehouseMapper;

    @Override
    public List<TransferStorageInfo>
        listTransferStorageInfoByCondition(TransferStorageInfoQueryReqDTO transferStorageInfoQueryReqDTO) {
        return transferStorageInfoMapper.listTransferStorageInfoByCondition(transferStorageInfoQueryReqDTO);
    }

    @Override
    public List<TransferStorageAudit>
        listTransferStorageAuditByCondition(TransferStorageAuditQueryReqDTO transferStorageAuditQueryReqDTO) {
        return transferStorageAuditMapper.listTransferStorageAuditByCondition(transferStorageAuditQueryReqDTO);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> audit(TransferStorageAuditReqDTO transferStorageAuditReqDTO) {
        final TransferStorageInfo transferStorageInfoById =
            transferStorageInfoMapper.selectById(transferStorageAuditReqDTO.getTransferId());
        if (transferStorageInfoById != null
            && transferStorageInfoById.getStatus() == TransferStorageAuditStatusEnum.IN_REVIEW.getCode()) {
            // 移出仓库数据
            final ActionResult<StsWarehouse, StsWarehouse> check = transferStorageInfoComponent.check(transferStorageInfoById);
            if (!check.getStatus().isSuccess()) {
                return actionResultService.callBackResultFail(check.getStatus().getCode(),check.getStatus().getMessage());
            }
            StsWarehouse sourceWarehouse = check.getResult();
            StsWarehouse targeteWarehouse = check.getAttached();
            StsWarehouse sourceWarehouseUpdate = new StsWarehouse();
            sourceWarehouseUpdate.setId(sourceWarehouse.getId());
            sourceWarehouseUpdate.setCurrentWeight(
                sourceWarehouse.getCurrentWeight().subtract(transferStorageInfoById.getEstimatedQuantity()));
            warehouseMapper.updateById(sourceWarehouseUpdate);
            StsWarehouse targetWarehouseUpdate = new StsWarehouse();
            targetWarehouseUpdate.setId(targeteWarehouse.getId());
            targetWarehouseUpdate.setCurrentWeight(
                    targeteWarehouse.getCurrentWeight().add(transferStorageInfoById.getEstimatedQuantity()));
            warehouseMapper.updateById(targetWarehouseUpdate);
            TransferStorageInfo transferStorageInfo = new TransferStorageInfo();
            transferStorageInfo.setId(transferStorageAuditReqDTO.getTransferId());
            transferStorageInfo.setStatus(transferStorageAuditReqDTO.getStatus());
            transferStorageInfoMapper.updateById(transferStorageInfo);
            TransferStorageAudit transferStorageAudit = new TransferStorageAudit();
            transferStorageAudit.setApprovalOpinion(transferStorageAuditReqDTO.getApprovalOpinion());
            transferStorageInfoComponent.saveAuditRecord(transferStorageInfo, transferStorageAudit,
                TransferStorageAuditStatusEnum.fromCode(transferStorageInfo.getStatus()));
            return actionResultService.callBackResult();
        }
        return actionResultService.callBackResultFail("当前单据状态不允许审批");
    }
}
