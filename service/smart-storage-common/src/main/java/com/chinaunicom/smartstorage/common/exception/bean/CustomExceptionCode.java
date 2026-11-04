package com.chinaunicom.smartstorage.common.exception.bean;

/**
 * 自定义异常编号
 *
 * @author liuchink
 * @since 2018/9/19 21:51
 */
public class CustomExceptionCode {
    private CustomExceptionCode() {

    }

    /**
     * 404
     */
    public static final String NO_HANDLER_FOUND_EXCEPTION_CODE = "404";
    public static final String NO_HANDLER_FOUND_EXCEPTION_CODE_MESSAGE = "404资源不存在";
    /**
     * 没有权限
     */
    public static final String ACCESS_DENIED_EXCEPTION_CODE = "403";
    public static final String ACCESS_DENIED_EXCEPTION_CODE_MESSAGE = "没有访问权限";
    /**
     * 默认异常
     */
    public static final String DEFAULT_EXCEPTION_CODE = "500";
    public static final String DEFAULT_EXCEPTION_MESSAGE = "系统出现异常，请联系管理员";
    /**
     * 请求头
     */
    public static final String MISSING_REQUEST_HEADER_EXCEPTION_CODE = "9100";
    public static final String MISSING_REQUEST_HEADER_EXCEPTION_CODE_MESSAGE = "请求头信息获取失败";
    /**
     * jwt过期
     */
    public static final String JWT_EXPIRED_EXCEPTION_CODE = "9101";
    public static final String JWT_EXPIRED_EXCEPTION_CODE_MESSAGE = "token已过期";
    /**
     * JWT验证失败
     */
    public static final String JWT_EXCEPTION_CODE = "9102";
    public static final String JWT_EXCEPTION_CODE_MESSAGE = "token校验失败";
    /**
     * 参数校验失败
     */
    public static final String METHOD_ARGUMENT_NOT_VALID_EXCEPTION_CODE = "9103";
    /**
     * 参数校验
     */
    public static final String ILLEGAL_ARGUMENT_EXCEPTION_CODE = "9104";
    public static final String ILLEGAL_ARGUMENT_EXCEPTION_CODE_MESSAGE = "非法参数异常";
    /**
     * json解析
     */
    public static final String JSON_PARSE_EXCEPTION_CODE = "9105";
    public static final String JSON_PARSE_EXCEPTION_CODE_MESSAGE = "JSON解析失败";
    /**
     * http请求参数
     */
    public static final String HTTP_MESSAGE_EXCEPTION_CODE = "9106";
    public static final String HTTP_MESSAGE_EXCEPTION_CODE_MESSAGE = "参数内容解析失败，请检查数据格式";
    /**
     * 表单重复提交
     */
    public static final String SUBMIT_SAME_EXCEPTION_CODE = "9107";
    public static final String SUBMIT_SAME_EXCEPTION_CODE_MESSAGE = "请勿重复提交";
    /**
     * 验证码
     */
    public static final String VERIFY_CODE_EXCEPTION_CODE = "9108";
    public static final String VERIFY_CODE_EXCEPTION_CODE_MESSAGE = "验证码错误";
    /**
     * JWT验证失败
     */
    public static final String JWT_HEADER_EXCEPTION_CODE = "9109";
    public static final String JWT_HEADER_EXCEPTION_CODE_MESSAGE = "未获取到token信息";
    /**
     * 空指针错误
     */
    public static final String NULL_POINTER_EXCEPTION_CODE = "9110";
    public static final String NULL_POINTER_EXCEPTION_CODE_MESSAGE = "空指针错误";
    /**
     * 数组越界错误
     */
    public static final String INDEX_OUT_OF_BOUNDS_EXCEPTION_CODE = "9111";
    public static final String INDEX_OUT_OF_BOUNDS_EXCEPTION_CODE_MESSAGE = "数组越界错误";
    /**
     * 文件未找到
     */
    public static final String FILE_NOT_FOUND_EXCEPTION_CODE = "9112";
    public static final String FILE_NOT_FOUND_EXCEPTION_CODE_MESSAGE = "文件未找到";
    /**
     * 服务接口请求失败
     */
    public static final String DATA_INTEGRITY_VIOLATION_EXCEPTION_CODE = "9113";
    public static final String DATA_INTEGRITY_VIOLATION_EXCEPTION_CODE_MESSAGE = "违反数据约束，请检查数据";
    public static final String HTTP_PREQUEST_METHOD_NOT_SUPPORTED_EXCEPTION_CODE = "9114";
    public static final String HTTP_PREQUEST_METHOD_NOT_SUPPORTED_EXCEPTION_CODE_MESSAGE = "不支持的请求方式";
    public static final String USER_LOGIN_EXPIRE_EXCEPTION_CODE = "403";
    public static final String USER_LOGIN_EXPIRE_EXCEPTION_CODE_MESSAGE = "登录信息已过期，请重新登录";

}
