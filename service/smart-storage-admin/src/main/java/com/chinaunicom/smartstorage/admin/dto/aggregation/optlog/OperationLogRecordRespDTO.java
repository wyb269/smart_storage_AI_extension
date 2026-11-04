package com.chinaunicom.smartstorage.admin.dto.aggregation.optlog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志
 *
 * @author liuchink
 * @since 2022/7/6 13:45
 */
@Data
public class OperationLogRecordRespDTO {
    /**
     * 主键ID
     */
    @Schema(description = "id", nullable = true, example = "123456")
    private String id;

    /**
     * 模块标题
     */
    @Schema(description = "模块标题", nullable = true, example = "123456")
    private String title;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型", nullable = true, example = "123456")
    private String businessType;

    /**
     * 方法名称
     */
    @Schema(description = "方法名称", nullable = true, example = "123456")
    private String method;

    /**
     * 请求方式
     */
    @Schema(description = "请求方式", nullable = true, example = "123456")
    private String requestMethod;

    /**
     * 操作类别
     */
    @Schema(description = "操作类别", nullable = true, example = "123456")
    private String operType;

    /**
     * 操作人
     */
    @Schema(description = "操作人", nullable = true, example = "123456")
    private String operName;

    /**
     * 请求URL
     */
    @Schema(description = "请求URL", nullable = true, example = "123456")
    private String operUrl;

    /**
     * 主机地址
     */
    @Schema(description = "主机地址", nullable = true, example = "123456")
    private String operIp;

    /**
     * 请求参数
     */
    @Schema(description = "请求参数", nullable = true, example = "123456")
    private String operParam;

    /**
     * 返回内容
     */
    @Schema(description = "返回内容", nullable = true, example = "123456")
    private String jsonResult;

    /**
     * 操作状态
     */
    @Schema(description = "操作状态", nullable = true, example = "123456")
    private String state;

    /**
     * 异常信息
     */
    @Schema(description = "异常信息", nullable = true, example = "123456")
    private String errorMsg;

    /**
     * 操作时间
     */
    @Schema(description = "操作时间", nullable = true, example = "123456")
    private LocalDateTime operTime;
    /**
     * token
     */
    // @Schema(description = "token", nullable = true, example = "123456")
    // private String token;

}
