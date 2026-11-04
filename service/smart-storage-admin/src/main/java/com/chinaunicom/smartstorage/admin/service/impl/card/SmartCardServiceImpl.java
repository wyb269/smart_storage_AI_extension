package com.chinaunicom.smartstorage.admin.service.impl.card;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.component.CodeGeneratorComponent;
import com.chinaunicom.smartstorage.admin.component.SwipeCardComponent;
import com.chinaunicom.smartstorage.admin.dto.card.SmartCardQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.card.SwipeCardReqDTO;
import com.chinaunicom.smartstorage.admin.dto.card.SwipeCardRespDTO;
import com.chinaunicom.smartstorage.admin.entity.card.SmartCard;
import com.chinaunicom.smartstorage.admin.enumbean.TaskStatusEnum;
import com.chinaunicom.smartstorage.admin.mapper.card.SmartCardMapper;
import com.chinaunicom.smartstorage.admin.service.inter.card.SmartCardService;
import com.chinaunicom.smartstorage.common.enumbean.OptEnum;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * (SmartCard)表服务实现类
 *
 * @author liuchink
 * @since 2026-01-22 13:35:41
 */
@Slf4j
@Service
public class SmartCardServiceImpl implements SmartCardService {
    @Resource
    private SmartCardMapper smartCardMapper;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private SwipeCardComponent swipeCardComponent;
    @Resource
    private CodeGeneratorComponent codeGeneratorComponent;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveSmartCard(SmartCard smartCard) {
        ActionResult<String, String> actionResult = check(smartCard, OptEnum.ADD);
        if (actionResult.getStatus().isSuccess()) {
            smartCard.setCardNo(codeGeneratorComponent.getWorkFlowSmartCard());
            smartCardMapper.insert(smartCard);
            actionResult = actionResultService.callBackResult();
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateSmartCard(SmartCard smartCard) {
        ActionResult<String, String> actionResult = check(smartCard, OptEnum.UPDATE);
        if (actionResult.getStatus().isSuccess()) {
            smartCardMapper.updateById(smartCard);
            actionResult = actionResultService.callBackResult();
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteSmartCard(List<SmartCard> listSmartCard) {
        for (SmartCard smartCard : listSmartCard) {
            final SmartCard smartCardById = smartCardMapper.selectById(smartCard.getId());
            if (smartCardById != null && smartCardById.getState() == 1) {
                return actionResultService.callBackResultFail("无法删除已发卡的数据");
            }
        }
        smartCardMapper.deleteByIds(listSmartCard);
        return actionResultService.callBackResult();
    }

    @Override
    public List<SmartCard> listSmartCardByCondition(SmartCardQueryReqDTO smartCardQueryReqDTO) {
        return smartCardMapper.listSmartCardByCondition(smartCardQueryReqDTO);
    }

    @Override
    public SmartCard getSmartCardById(String id) {
        return smartCardMapper.selectById(id);
    }

    @Override
    public ActionResult<SwipeCardRespDTO, String> inSwipeCard(SwipeCardReqDTO swipeCardReqDTO) {
        TaskStatusEnum taskStatusEnum = TaskStatusEnum.fromCode(swipeCardReqDTO.getProcessStep());
        return switch (taskStatusEnum) {
            case TaskStatusEnum.LOGGED_IN -> swipeCardComponent.inLogin(swipeCardReqDTO);
            case TaskStatusEnum.SAMPLED -> swipeCardComponent.inSampleCollection(swipeCardReqDTO);
            case TaskStatusEnum.INSPECTED -> swipeCardComponent.inQualityCheck(swipeCardReqDTO);
            case TaskStatusEnum.WEIGHED -> swipeCardComponent.inWeight(swipeCardReqDTO);
            case TaskStatusEnum.WAREHOUSED -> swipeCardComponent.inDatawarehouse(swipeCardReqDTO);
            case TaskStatusEnum.SETTLED -> swipeCardComponent.inSettlement(swipeCardReqDTO);
            case TaskStatusEnum.LOGOUT -> swipeCardComponent.inLogout(swipeCardReqDTO);
            default -> actionResultService.callBackResultFail("未定义的刷卡类型");
        };
    }

    @Override
    public ActionResult<SwipeCardRespDTO, String> outSwipeCard(SwipeCardReqDTO swipeCardReqDTO) {
        TaskStatusEnum taskStatusEnum = TaskStatusEnum.fromCode(swipeCardReqDTO.getProcessStep());
        return switch (taskStatusEnum) {
            case TaskStatusEnum.LOGGED_IN -> swipeCardComponent.outLogin(swipeCardReqDTO);
            case TaskStatusEnum.WEIGHED -> swipeCardComponent.outWeight(swipeCardReqDTO);
            case TaskStatusEnum.WAREHOUSED -> swipeCardComponent.outDatawarehouse(swipeCardReqDTO);
            case TaskStatusEnum.SETTLED -> swipeCardComponent.outSettlement(swipeCardReqDTO);
            case TaskStatusEnum.LOGOUT -> swipeCardComponent.outLogout(swipeCardReqDTO);
            default -> actionResultService.callBackResultFail("未定义的刷卡类型");
        };
    }

    private ActionResult<String, String> check(SmartCard smartCard, OptEnum optEnum) {
        final SmartCard smartCardCode = smartCardMapper
            .selectOne(Wrappers.lambdaQuery(SmartCard.class).eq(SmartCard::getCardNo, smartCard.getCardNo()));
        if (smartCardCode != null && !smartCardCode.getId().equals(smartCard.getId())) {
            return actionResultService.callBackResultFail("智能卡编号不能重复");
        }
        final SmartCard smartCardPhysicalCardNumber = smartCardMapper.selectOne(Wrappers.lambdaQuery(SmartCard.class)
            .eq(SmartCard::getPhysicalCardNumber, smartCard.getPhysicalCardNumber()));
        if (smartCardPhysicalCardNumber != null && !smartCardPhysicalCardNumber.getId().equals(smartCard.getId())) {
            return actionResultService.callBackResultFail("物理编号不能重复");
        }
        return actionResultService.callBackResult();
    }
}
