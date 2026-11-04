package com.chinaunicom.smartstorage.admin.component;

import cn.hutool.core.convert.Convert;
import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.entity.aggregation.StsWarehouse;
import com.chinaunicom.smartstorage.admin.entity.transfer.TransferStorageAudit;
import com.chinaunicom.smartstorage.admin.entity.transfer.TransferStorageInfo;
import com.chinaunicom.smartstorage.admin.enumbean.TransferStorageAuditStatusEnum;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.WarehouseMapper;
import com.chinaunicom.smartstorage.admin.mapper.transfer.TransferStorageAuditMapper;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author liugang
 * @since 2026/3/12 9:01
 */
@Component
public class TransferStorageInfoComponent {
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private TransferStorageAuditMapper transferStorageAuditMapper;
    @Resource
    private WarehouseMapper warehouseMapper;
    @Resource
    private ActionResultService actionResultService;

    public ActionResult<StsWarehouse, StsWarehouse> check(TransferStorageInfo transferStorageInfo) {
        // 移出仓库数据
        final StsWarehouse sourceWarehouse = warehouseMapper.selectById(transferStorageInfo.getSourceWarehouseId());
        if (sourceWarehouse == null) {
            return actionResultService.callBackResultFail("移出仓库不存在");
        }
        // 移入仓库数据
        final StsWarehouse targeteWarehouse = warehouseMapper.selectById(transferStorageInfo.getTargetWarehouseId());
        if (targeteWarehouse == null) {
            return actionResultService.callBackResultFail("移入仓库不存在");
        }
        if (transferStorageInfo.getSourceWarehouseId().equals(transferStorageInfo.getTargetWarehouseId())) {
            return actionResultService.callBackResultFail("移出仓库和移入仓库不能相同");
        }
        if (sourceWarehouse.getState() != 1) {
            return actionResultService.callBackResultFail("移出仓库状态禁用");
        }
        if (sourceWarehouse.getMaxWeight().compareTo(transferStorageInfo.getEstimatedQuantity()) < 0
            || sourceWarehouse.getCurrentWeight().compareTo(transferStorageInfo.getEstimatedQuantity()) < 0) {
            return actionResultService.callBackResultFail("移出仓库容量不足");
        }
        if (targeteWarehouse.getState() != 1) {
            return actionResultService.callBackResultFail("移入仓库状态禁用");
        }
        if (targeteWarehouse.getCurrentWeight().add(transferStorageInfo.getEstimatedQuantity())
            .compareTo(targeteWarehouse.getMaxWeight()) > 0) {
            return actionResultService.callBackResultFail("移入仓库容量超出最大容量");
        }
        return actionResultService.callBackResult(sourceWarehouse, targeteWarehouse);
    }

    public void saveAuditRecord(TransferStorageInfo transferStorageInfo,
        TransferStorageAuditStatusEnum transferStorageAuditStatusEnum) {
        TransferStorageAudit transferStorageAudit = new TransferStorageAudit();
        transferStorageAudit.setId(Convert.toStr(uidGenerator.getUID()));
        transferStorageAudit.setTransferId(transferStorageInfo.getId());
        transferStorageAudit.setStatus(transferStorageAuditStatusEnum.getCode());
        transferStorageAudit.setAddTime(LocalDateTime.now());
        transferStorageAudit.setAddUser(jwtTokenComponent.getUser().getId());
        transferStorageAuditMapper.insert(transferStorageAudit);
    }

    public void saveAuditRecord(TransferStorageInfo transferStorageInfo, TransferStorageAudit transferStorageAudit,
        TransferStorageAuditStatusEnum transferStorageAuditStatusEnum) {
        transferStorageAudit.setId(Convert.toStr(uidGenerator.getUID()));
        transferStorageAudit.setTransferId(transferStorageInfo.getId());
        transferStorageAudit.setStatus(transferStorageAuditStatusEnum.getCode());
        transferStorageAudit.setAddTime(LocalDateTime.now());
        transferStorageAudit.setAddUser(jwtTokenComponent.getUser().getId());
        transferStorageAudit.setAuditTime(LocalDateTime.now());
        transferStorageAudit.setAuditUser(transferStorageAudit.getAddUser());
        transferStorageAudit.setApprovalOpinion(transferStorageAudit.getApprovalOpinion());
        transferStorageAuditMapper.insert(transferStorageAudit);
    }
}
