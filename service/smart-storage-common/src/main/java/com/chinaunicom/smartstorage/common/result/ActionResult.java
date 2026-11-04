package com.chinaunicom.smartstorage.common.result;

import com.alibaba.fastjson2.annotation.JSONField;

import lombok.Data;

/**
 * 前台页面返回结果类
 *
 * @author liuchink
 * @since 2017/12/18 11:00
 */
@Data
public class ActionResult<R, A> {

    // @Schema(description = "状态", nullable = true, example = "123456")
    @JSONField(name = "status")
    private StatusResult status;
    // @Schema(description = "结果", nullable = true, example = "123456")
    @JSONField(name = "result")
    private R result;
    // @Schema(description = "其他", nullable = true, example = "123456")
    @JSONField(name = "attached")
    private A attached;

    public ActionResult() {
        this.status = new StatusResult();
    }
}
