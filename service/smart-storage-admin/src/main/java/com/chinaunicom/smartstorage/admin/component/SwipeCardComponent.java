package com.chinaunicom.smartstorage.admin.component;

import cn.hutool.core.util.StrUtil;
import com.chinaunicom.smartstorage.admin.dto.card.SwipeCardReqDTO;
import com.chinaunicom.smartstorage.admin.dto.card.SwipeCardRespDTO;
import com.chinaunicom.smartstorage.admin.enumbean.TaskStatusEnum;
import com.chinaunicom.smartstorage.admin.mapper.card.SmartCardMapper;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * @author liugang
 * @since 2026/1/23 8:53
 */
@Component
public class SwipeCardComponent {
    @Resource
    private SmartCardMapper smartCardMapper;
    @Resource
    private ActionResultService actionResultService;

    /**
     *
     * 入库登入
     *
     * @param swipeCardReqDTO swipeCardReqDTO
     *
     * @return result.com.chinaunicom.smartstorage.common.ActionResult<card.dto.com.chinaunicom.smartstorage.admin.SwipeCardRespDTO,java.lang.String>
     * @author liugang
     * @since 2026/1/23 9:22
     */
    public ActionResult<SwipeCardRespDTO, String> inLogin(SwipeCardReqDTO swipeCardReqDTO) {
        final ActionResult<SwipeCardRespDTO, String> cardInfoActionResult = getCardInfo(swipeCardReqDTO);
        if (cardInfoActionResult.getStatus().isSuccess()) {
            SwipeCardRespDTO swipeCardRespDTO = cardInfoActionResult.getResult();
            if (swipeCardRespDTO.getState() == 1) {
                return actionResultService.callBackResultFail("当前卡片还未登出，请先登出后再使用");
            }
            return actionResultService.callBackResult(swipeCardRespDTO);
        }
        return cardInfoActionResult;
    }

    /**
     *
     * 出库登入
     *
     * @param swipeCardReqDTO swipeCardReqDTO
     *
     * @return result.com.chinaunicom.smartstorage.common.ActionResult<card.dto.com.chinaunicom.smartstorage.admin.SwipeCardRespDTO,java.lang.String>
     * @author liugang
     * @since 2026/1/23 9:22
     */
    public ActionResult<SwipeCardRespDTO, String> outLogin(SwipeCardReqDTO swipeCardReqDTO) {
        final ActionResult<SwipeCardRespDTO, String> cardInfoActionResult = getCardInfo(swipeCardReqDTO);
        if (cardInfoActionResult.getStatus().isSuccess()) {
            SwipeCardRespDTO swipeCardRespDTO = cardInfoActionResult.getResult();
            if (swipeCardRespDTO.getState() == 1) {
                return actionResultService.callBackResultFail("当前卡片还未登出，请先登出后再使用");
            }
            return actionResultService.callBackResult(swipeCardRespDTO);
        }
        return cardInfoActionResult;
    }

    /**
     *
     * 入库扦样
     *
     * @param swipeCardReqDTO swipeCardReqDTO
     *
     * @return result.com.chinaunicom.smartstorage.common.ActionResult<card.dto.com.chinaunicom.smartstorage.admin.SwipeCardRespDTO,java.lang.String>
     * @author liugang
     * @since 2026/1/23 9:34
     */
    public ActionResult<SwipeCardRespDTO, String> inSampleCollection(SwipeCardReqDTO swipeCardReqDTO) {
        final ActionResult<SwipeCardRespDTO, String> cardInfoActionResult = getCardInfo(swipeCardReqDTO);
        if (cardInfoActionResult.getStatus().isSuccess()) {
            SwipeCardRespDTO swipeCardRespDTO = cardInfoActionResult.getResult();
            if (swipeCardRespDTO.getState() == 2) {
                return actionResultService.callBackResultFail("当前卡片还未登入，请先登入后再使用");
            }
            if (!StrUtil.equals(swipeCardRespDTO.getJobStatus(), TaskStatusEnum.LOGGED_IN.getCode())) {
                return actionResultService.callBackResultFail("当前作业状态禁止入库扦样操作");
            }
            return actionResultService.callBackResult(swipeCardRespDTO);
        }
        return cardInfoActionResult;
    }

    /**
     *
     * 入库质检
     *
     * @param swipeCardReqDTO swipeCardReqDTO
     *
     * @return result.com.chinaunicom.smartstorage.common.ActionResult<card.dto.com.chinaunicom.smartstorage.admin.SwipeCardRespDTO,java.lang.String>
     * @author liugang
     * @since 2026/1/23 9:34
     */
    public ActionResult<SwipeCardRespDTO, String> inQualityCheck(SwipeCardReqDTO swipeCardReqDTO) {
        final ActionResult<SwipeCardRespDTO, String> cardInfoActionResult = getCardInfo(swipeCardReqDTO);
        if (cardInfoActionResult.getStatus().isSuccess()) {
            SwipeCardRespDTO swipeCardRespDTO = cardInfoActionResult.getResult();
            if (swipeCardRespDTO.getState() == 2) {
                return actionResultService.callBackResultFail("当前卡片还未登入，请先登入后再使用");
            }
            if (!StrUtil.equals(swipeCardRespDTO.getJobStatus(), TaskStatusEnum.SAMPLED.getCode())) {
                return actionResultService.callBackResultFail("当前作业状态禁止入库质检操作");
            }
            return actionResultService.callBackResult(swipeCardRespDTO);
        }
        return cardInfoActionResult;
    }

    /**
     *
     * 入库称重
     *
     * @param swipeCardReqDTO swipeCardReqDTO
     *
     * @return result.com.chinaunicom.smartstorage.common.ActionResult<card.dto.com.chinaunicom.smartstorage.admin.SwipeCardRespDTO,java.lang.String>
     * @author liugang
     * @since 2026/1/23 9:34
     */
    public ActionResult<SwipeCardRespDTO, String> inWeight(SwipeCardReqDTO swipeCardReqDTO) {
        final ActionResult<SwipeCardRespDTO, String> cardInfoActionResult = getCardInfo(swipeCardReqDTO);
        if (cardInfoActionResult.getStatus().isSuccess()) {
            SwipeCardRespDTO swipeCardRespDTO = cardInfoActionResult.getResult();
            if (swipeCardRespDTO.getState() == 2) {
                return actionResultService.callBackResultFail("当前卡片还未登入，请先登入后再使用");
            }
            if (!StrUtil.equals(swipeCardRespDTO.getJobStatus(), TaskStatusEnum.INSPECTED.getCode())
                && !StrUtil.equals(swipeCardRespDTO.getJobStatus(), TaskStatusEnum.WAREHOUSED.getCode())) {
                return actionResultService.callBackResultFail("当前作业状态禁止入库称重操作");
            }
            return actionResultService.callBackResult(swipeCardRespDTO);
        }
        return cardInfoActionResult;
    }

    /**
     *
     * 出库称重
     *
     * @param swipeCardReqDTO swipeCardReqDTO
     *
     * @return result.com.chinaunicom.smartstorage.common.ActionResult<card.dto.com.chinaunicom.smartstorage.admin.SwipeCardRespDTO,java.lang.String>
     * @author liugang
     * @since 2026/1/23 9:34
     */
    public ActionResult<SwipeCardRespDTO, String> outWeight(SwipeCardReqDTO swipeCardReqDTO) {
        final ActionResult<SwipeCardRespDTO, String> cardInfoActionResult = getCardInfo(swipeCardReqDTO);
        if (cardInfoActionResult.getStatus().isSuccess()) {
            SwipeCardRespDTO swipeCardRespDTO = cardInfoActionResult.getResult();
            if (swipeCardRespDTO.getState() == 2) {
                return actionResultService.callBackResultFail("当前卡片还未登入，请先登入后再使用");
            }
            if (!StrUtil.equals(swipeCardRespDTO.getJobStatus(), TaskStatusEnum.LOGGED_IN.getCode())
                && !StrUtil.equals(swipeCardRespDTO.getJobStatus(), TaskStatusEnum.WAREHOUSED.getCode())) {
                return actionResultService.callBackResultFail("当前作业状态禁止出库称重操作");
            }
            return actionResultService.callBackResult(swipeCardRespDTO);
        }
        return cardInfoActionResult;
    }

    /**
     *
     * 入库值仓
     *
     * @param swipeCardReqDTO swipeCardReqDTO
     *
     * @return result.com.chinaunicom.smartstorage.common.ActionResult<card.dto.com.chinaunicom.smartstorage.admin.SwipeCardRespDTO,java.lang.String>
     * @author liugang
     * @since 2026/1/23 9:34
     */
    public ActionResult<SwipeCardRespDTO, String> inDatawarehouse(SwipeCardReqDTO swipeCardReqDTO) {
        final ActionResult<SwipeCardRespDTO, String> cardInfoActionResult = getCardInfo(swipeCardReqDTO);
        if (cardInfoActionResult.getStatus().isSuccess()) {
            SwipeCardRespDTO swipeCardRespDTO = cardInfoActionResult.getResult();
            if (swipeCardRespDTO.getState() == 2) {
                return actionResultService.callBackResultFail("当前卡片还未登入，请先登入后再使用");
            }
            if (!StrUtil.equals(swipeCardRespDTO.getJobStatus(), TaskStatusEnum.WEIGHED.getCode())) {
                return actionResultService.callBackResultFail("当前作业状态禁止入库值仓操作");
            }
            return actionResultService.callBackResult(swipeCardRespDTO);
        }
        return cardInfoActionResult;
    }

    /**
     *
     * 出库值仓
     *
     * @param swipeCardReqDTO swipeCardReqDTO
     *
     * @return result.com.chinaunicom.smartstorage.common.ActionResult<card.dto.com.chinaunicom.smartstorage.admin.SwipeCardRespDTO,java.lang.String>
     * @author liugang
     * @since 2026/1/23 9:34
     */
    public ActionResult<SwipeCardRespDTO, String> outDatawarehouse(SwipeCardReqDTO swipeCardReqDTO) {
        final ActionResult<SwipeCardRespDTO, String> cardInfoActionResult = getCardInfo(swipeCardReqDTO);
        if (cardInfoActionResult.getStatus().isSuccess()) {
            SwipeCardRespDTO swipeCardRespDTO = cardInfoActionResult.getResult();
            if (swipeCardRespDTO.getState() == 2) {
                return actionResultService.callBackResultFail("当前卡片还未登入，请先登入后再使用");
            }
            if (!StrUtil.equals(swipeCardRespDTO.getJobStatus(), TaskStatusEnum.TARE_WEIGHED.getCode())) {
                return actionResultService.callBackResultFail("当前作业状态禁止出库值仓操作");
            }
            return actionResultService.callBackResult(swipeCardRespDTO);
        }
        return cardInfoActionResult;
    }

    /**
     *
     * 入库结算
     *
     * @param swipeCardReqDTO swipeCardReqDTO
     *
     * @return result.com.chinaunicom.smartstorage.common.ActionResult<card.dto.com.chinaunicom.smartstorage.admin.SwipeCardRespDTO,java.lang.String>
     * @author liugang
     * @since 2026/1/23 9:34
     */
    public ActionResult<SwipeCardRespDTO, String> inSettlement(SwipeCardReqDTO swipeCardReqDTO) {
        final ActionResult<SwipeCardRespDTO, String> cardInfoActionResult = getCardInfo(swipeCardReqDTO);
        if (cardInfoActionResult.getStatus().isSuccess()) {
            SwipeCardRespDTO swipeCardRespDTO = cardInfoActionResult.getResult();
            if (swipeCardRespDTO.getState() == 2) {
                return actionResultService.callBackResultFail("当前卡片还未登入，请先登入后再使用");
            }
            if (!StrUtil.equals(swipeCardRespDTO.getJobStatus(), TaskStatusEnum.TARE_WEIGHED.getCode())) {
                return actionResultService.callBackResultFail("当前作业状态禁止入库结算操作");
            }
            return actionResultService.callBackResult(swipeCardRespDTO);
        }
        return cardInfoActionResult;
    }

    /**
     *
     * 出库结算
     *
     * @param swipeCardReqDTO swipeCardReqDTO
     *
     * @return result.com.chinaunicom.smartstorage.common.ActionResult<card.dto.com.chinaunicom.smartstorage.admin.SwipeCardRespDTO,java.lang.String>
     * @author liugang
     * @since 2026/1/23 9:34
     */
    public ActionResult<SwipeCardRespDTO, String> outSettlement(SwipeCardReqDTO swipeCardReqDTO) {
        final ActionResult<SwipeCardRespDTO, String> cardInfoActionResult = getCardInfo(swipeCardReqDTO);
        if (cardInfoActionResult.getStatus().isSuccess()) {
            SwipeCardRespDTO swipeCardRespDTO = cardInfoActionResult.getResult();
            if (swipeCardRespDTO.getState() == 2) {
                return actionResultService.callBackResultFail("当前卡片还未登入，请先登入后再使用");
            }
            if (!StrUtil.equals(swipeCardRespDTO.getJobStatus(), TaskStatusEnum.WEIGHED.getCode())) {
                return actionResultService.callBackResultFail("当前作业状态禁止出库结算操作");
            }
            return actionResultService.callBackResult(swipeCardRespDTO);
        }
        return cardInfoActionResult;
    }

    /**
     *
     * 入库登出
     *
     * @param swipeCardReqDTO swipeCardReqDTO
     *
     * @return result.com.chinaunicom.smartstorage.common.ActionResult<card.dto.com.chinaunicom.smartstorage.admin.SwipeCardRespDTO,java.lang.String>
     * @author liugang
     * @since 2026/1/23 9:34
     */
    public ActionResult<SwipeCardRespDTO, String> inLogout(SwipeCardReqDTO swipeCardReqDTO) {
        final ActionResult<SwipeCardRespDTO, String> cardInfoActionResult = getCardInfo(swipeCardReqDTO);
        if (cardInfoActionResult.getStatus().isSuccess()) {
            SwipeCardRespDTO swipeCardRespDTO = cardInfoActionResult.getResult();
            if (swipeCardRespDTO.getState() == 2) {
                return actionResultService.callBackResultFail("当前卡片还未登入，请先登入后再使用");
            }
            return actionResultService.callBackResult(swipeCardRespDTO);
        }
        return cardInfoActionResult;
    }

    /**
     *
     * 出库登出
     *
     * @param swipeCardReqDTO swipeCardReqDTO
     *
     * @return result.com.chinaunicom.smartstorage.common.ActionResult<card.dto.com.chinaunicom.smartstorage.admin.SwipeCardRespDTO,java.lang.String>
     * @author liugang
     * @since 2026/1/23 9:34
     */
    public ActionResult<SwipeCardRespDTO, String> outLogout(SwipeCardReqDTO swipeCardReqDTO) {
        final ActionResult<SwipeCardRespDTO, String> cardInfoActionResult = getCardInfo(swipeCardReqDTO);
        if (cardInfoActionResult.getStatus().isSuccess()) {
            SwipeCardRespDTO swipeCardRespDTO = cardInfoActionResult.getResult();
            if (swipeCardRespDTO.getState() == 2) {
                return actionResultService.callBackResultFail("当前卡片还未登入，请先登入后再使用");
            }
            return actionResultService.callBackResult(swipeCardRespDTO);
        }
        return cardInfoActionResult;
    }

    private ActionResult<SwipeCardRespDTO, String> getCardInfo(SwipeCardReqDTO swipeCardReqDTO) {
        final SwipeCardRespDTO swipeCardRespDTO = smartCardMapper.swipeCard(swipeCardReqDTO);
        if (swipeCardRespDTO == null) {
            return actionResultService.callBackResultFail("卡片信息不存在");
        }
        return actionResultService.callBackResult(swipeCardRespDTO);
    }
}
