package com.chinaunicom.smartstorage.common.result;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 前台页面结果
 *
 * @author liuchink
 * @since 2017/12/18 11:19
 */
@Component
public class ActionResultService {

    /**
     * 封装统一前台页面返回
     *
     * @param success success
     * @param result result
     * @param code code
     * @param message message
     * @param attachedResult attachedResult
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2017/08/07 8:47
     */

    public <R, A> ActionResult<R, A> callBackResult(boolean success, String code, String message, R result,
        A attachedResult) {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(success);
        actionResult.getStatus().setMessage(message);
        actionResult.getStatus().setCode(code);
        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(result);
        // AttachedResult
        actionResult.setAttached(attachedResult);
        return actionResult;
    }

    /**
     * 封装统一前台页面返回
     *
     * @param success success
     * @param result result
     * @param code code
     * @param message message
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2017/08/07 8:47
     */

    public <R, A> ActionResult<R, A> callBackResult(boolean success, String code, String message, R result) {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(success);
        actionResult.getStatus().setMessage(message);
        actionResult.getStatus().setCode(code);

        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(result);
        // AttachedResult
        actionResult.setAttached(null);
        return actionResult;
    }

    /**
     * 封装统一前台页面返回
     *
     * @param success success
     * @param code code
     * @param message message
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2017/08/07 8:47
     */

    public <R, A> ActionResult<R, A> callBackResult(boolean success, String code, String message) {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(success);
        actionResult.getStatus().setMessage(message);
        actionResult.getStatus().setCode(code);

        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(null);
        // AttachedResult
        actionResult.setAttached(null);
        return actionResult;
    }

    /**
     * 封装统一前台页面返回
     *
     * @param result result
     * @param message message
     * @param attachedResult attachedResult
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2017/08/07 8:47
     */

    public <R, A> ActionResult<R, A> callBackResult(String message, R result, A attachedResult) {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(true);
        actionResult.getStatus().setMessage(message);

        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(result);
        // AttachedResult
        actionResult.setAttached(attachedResult);
        return actionResult;
    }

    /**
     * 封装统一前台页面返回
     *
     * @param result result
     * @param message message
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2017/08/07 8:47
     */

    public <R, A> ActionResult<R, A> callBackResult(String message, R result) {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(true);
        actionResult.getStatus().setMessage(message);

        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(result);
        // AttachedResult
        actionResult.setAttached(null);
        return actionResult;
    }

    /**
     * 封装统一前台页面返回
     *
     * @param result result
     * @param attachedResult attachedResult
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2017/08/07 8:47
     */

    public <R, A> ActionResult<R, A> callBackResult(R result, A attachedResult) {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(true);
        actionResult.getStatus().setMessage("请求成功");

        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(result);
        // AttachedResult
        actionResult.setAttached(attachedResult);
        return actionResult;
    }

    /**
     * 封装统一前台页面返回
     *
     * @param result result
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2017/08/07 8:47
     */

    public <R, A> ActionResult<R, A> callBackResult(R result) {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(true);
        actionResult.getStatus().setMessage("请求成功");

        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(result);
        // AttachedResult
        actionResult.setAttached(null);
        return actionResult;
    }

    /**
     * 封装统一前台页面返回
     *
     * @param message message
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2017/08/07 8:47
     */

    public <R, A> ActionResult<R, A> callBackResult(String message) {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(true);
        actionResult.getStatus().setMessage(message);

        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(null);
        // AttachedResult
        actionResult.setAttached(null);
        return actionResult;
    }

    /**
     * 封装统一前台页面返回
     *
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2017/08/07 8:47
     */

    public <R, A> ActionResult<R, A> callBackResult() {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(true);
        actionResult.getStatus().setMessage("请求成功");

        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(null);
        // AttachedResult
        actionResult.setAttached(null);
        return actionResult;
    }

    public <R, A> ActionResult<R, A> callBackResultFail(String message) {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(false);
        actionResult.getStatus().setMessage(message);
        actionResult.getStatus().setCode("-1");

        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(null);
        // AttachedResult
        actionResult.setAttached(null);
        return actionResult;
    }

    public <R, A> ActionResult<R, A> callBackResultFail() {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(false);
        actionResult.getStatus().setMessage("请求失败");
        actionResult.getStatus().setCode("-1");

        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(null);
        // AttachedResult
        actionResult.setAttached(null);
        return actionResult;
    }

    /**
     * 封装统一前台页面返回
     *
     * @param message message
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2017/08/07 8:47
     */

    public <R, A> ActionResult<R, A> callBackResultFail(String code, String message) {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(false);
        actionResult.getStatus().setMessage(message);
        actionResult.getStatus().setCode(code);

        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(null);
        // AttachedResult
        actionResult.setAttached(null);
        return actionResult;
    }

    /**
     * 封装统一前台页面返回
     *
     * @param message message
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2017/08/07 8:47
     */

    public <R, A> ActionResult<R, A> callBackResultFail(String code, String message, R result) {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(false);
        actionResult.getStatus().setMessage(message);
        actionResult.getStatus().setCode(code);

        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(result);
        // AttachedResult
        actionResult.setAttached(null);
        return actionResult;
    }

    /**
     * 封装统一前台页面返回
     *
     * @param message message
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2017/08/07 8:47
     */

    public <R, A> ActionResult<R, A> callBackResultFail(String code, String message, R result, A attached) {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(false);
        actionResult.getStatus().setMessage(message);
        actionResult.getStatus().setCode(code);

        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(result);
        // AttachedResult
        actionResult.setAttached(attached);
        return actionResult;
    }

    /**
     * 封装统一前台页面返回
     *
     * @param message message
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2017/08/07 8:47
     */

    public <R, A> ActionResult<R, A> callBackResultFail(String message, R result, A attached) {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(false);
        actionResult.getStatus().setMessage(message);
        actionResult.getStatus().setCode("-1");

        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(result);
        // AttachedResult
        actionResult.setAttached(attached);
        return actionResult;
    }

    /**
     * 封装统一前台页面返回
     *
     * @param message message
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2017/08/07 8:47
     */

    public <R, A> ActionResult<R, A> callBackResultFail(String message, R result) {
        ActionResult<R, A> actionResult = new ActionResult<>();
        // StatusResult
        actionResult.getStatus().setSuccess(false);
        actionResult.getStatus().setMessage(message);
        actionResult.getStatus().setCode("-1");

        actionResult.getStatus().setTimestamp(LocalDateTime.now());
        // Result
        actionResult.setResult(result);
        // AttachedResult
        actionResult.setAttached(null);
        return actionResult;
    }
}
