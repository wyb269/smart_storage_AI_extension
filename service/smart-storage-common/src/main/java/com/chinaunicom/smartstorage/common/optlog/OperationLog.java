package com.chinaunicom.smartstorage.common.optlog;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志
 *
 * @author liuchink
 * @since 2022/7/6 13:45
 */
@Data
public class OperationLog {

    /**
     * 模块标题
     */
    private String title;

    /**
     * 业务类型（0其它 1新增 2修改 3删除）
     */
    private String businessType;

    /**
     * 方法名称
     */
    private String method;

    /**
     * 请求方式
     */
    private String requestMethod;

    /**
     * 操作类别（0其它 1后台用户 2手机端用户）
     */
    private String operType;

    /**
     * 操作人
     */
    private String operName;

    /**
     * 请求URL
     */
    private String operUrl;

    /**
     * 主机地址
     */
    private String operIp;

    /**
     * 请求参数
     */
    private String operParam;

    /**
     * 返回内容
     */
    private String jsonResult;

    /**
     * 操作状态（0正常 1异常）
     */
    private String state;

    /**
     * 异常信息
     */
    private String errorMsg;

    /**
     * 操作时间
     */
    private LocalDateTime operTime;
    /**
     * 操作时间
     */
    private String token;
    private Long processTime;
}
