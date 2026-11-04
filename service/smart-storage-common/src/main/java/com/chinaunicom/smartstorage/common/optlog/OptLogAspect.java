package com.chinaunicom.smartstorage.common.optlog;

import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.fastjson2.JSON;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.jwt.JwtConstant;
import com.chinaunicom.smartstorage.common.jwt.JwtTokenUtils;
import com.chinaunicom.smartstorage.common.utils.IpUtils;
import com.liuchink.chinkutils.json.ChinkFastJson2Utils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

/**
 * 操作日志
 *
 * @author : liuchink
 * @since 2022/7/6 14:11
 */
@Aspect
@Component
@Slf4j
public class OptLogAspect {
    @Resource
    private HttpServletRequest httpServletRequest;
    @Resource
    private HttpServletResponse httpServletResponse;
    @Autowired(required = false)
    private OperationLogService operationLogService;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     *
     * 配置织入点
     *
     * @author liuchink
     * @since 2022/7/6 14:17
     */
    @Pointcut("@annotation(com.chinaunicom.smartstorage.common.optlog.OptLog)")
    public void operationLogCut() {}

    @Around("operationLogCut()")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object[] args = proceedingJoinPoint.getArgs();
        long startTime = System.currentTimeMillis();
        Object jsonResult;
        try {
            jsonResult = proceedingJoinPoint.proceed(args);
            long endTime = System.currentTimeMillis();
            handleLog(proceedingJoinPoint, null, ChinkFastJson2Utils.toJsonString(jsonResult), endTime - startTime);
        } catch (Exception e) {
            handleLog(proceedingJoinPoint, e, null, System.currentTimeMillis() - startTime);
            throw e;
        }
        return jsonResult;
    }

    /**
     * 处理
     *
     * @param joinPoint joinPoint
     * @param exception exception
     * @param jsonResult jsonResult
     *
     * @author liuchink
     * @since 2022/7/6 14:18
     */
    protected void handleLog(final JoinPoint joinPoint, final Exception exception, Object jsonResult,
        long processTime) {
        OptLog optLog = getAnnotationLog(joinPoint);
        if (optLog == null) {
            return;
        }
        String jwtToke = httpServletRequest.getHeader(JwtConstant.REQUEST_HEAD_TOKEN);
        String userName = "匿名用户";
        if (CharSequenceUtil.isNotBlank(jwtToke)) {
            try {
                userName = switch (optLog.businessType()) {
                    case MANAGE -> {
                        final OptLogUser optLogUser = getOptLogUser(optLog.businessType(),
                            JwtTokenUtils.getSubject(jwtToke, JwtConstant.JWT_ADMIN_SECRET));
                        if (optLogUser != null) {
                            yield optLogUser.getLoginId();
                        } else {
                            yield "未获取到登录信息";
                        }
                    }
                    case APPLET -> {
                        final OptLogUser optLogUser = getOptLogUser(optLog.businessType(),
                            JwtTokenUtils.getSubject(jwtToke, JwtConstant.JWT_APPLET_SECRET));
                        if (optLogUser != null) {
                            yield optLogUser.getLoginId();
                        } else {
                            yield "未获取到登录信息";
                        }
                    }
                    case APP -> {
                        final OptLogUser optLogUser = getOptLogUser(optLog.businessType(),
                            JwtTokenUtils.getSubject(jwtToke, JwtConstant.JWT_APP_SECRET));
                        if (optLogUser != null) {
                            yield optLogUser.getLoginId();
                        } else {
                            yield "未获取到登录信息";
                        }
                    }
                    default -> "未指定业务类型";
                };
            } catch (Exception ex) {
                userName = "异常用户";
                log.error(SystemConstant.LOG_ERROR_TITLE, ex.getMessage(), ex);
            }
        }
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setState(OptLogBusinessStatus.SUCCESS.getValue());
            operationLog.setOperIp(IpUtils.getRemoteHost(httpServletRequest));
            operationLog.setJsonResult(ChinkFastJson2Utils.toJsonString(jsonResult));
            operationLog.setOperUrl(httpServletRequest.getRequestURI());
            operationLog.setToken(jwtToke);
            operationLog.setOperName(userName);
            if (exception != null) {
                operationLog.setState(OptLogBusinessStatus.FAIL.getValue());
                operationLog.setErrorMsg(StringUtils.substring(exception.getMessage(), 0, 3500));
            }
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operationLog.setMethod(className + "." + methodName);
            operationLog.setRequestMethod(httpServletRequest.getMethod());
            getControllerMethodDescription(joinPoint, optLog, operationLog);
            operationLog.setOperTime(LocalDateTime.now());
            operationLog.setProcessTime(processTime);
            if (operationLogService != null) {
                operationLogService.saveOperationLog(optLog, operationLog);
            }
        } catch (Exception ex) {
            log.error(SystemConstant.LOG_ERROR_TITLE, ex.getMessage(), ex);
        }
    }

    /**
     *
     * 是否存在注解，如果存在就获取
     *
     * @param joinPoint joinPoint
     *
     * @return com.liuchink.chinkframe.cloud.common.aop.annotation.optlog.OperationLog
     * @author liuchink
     * @since 2022/7/6 14:20
     */
    private OptLog getAnnotationLog(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature)signature;
        Method method = methodSignature.getMethod();

        if (method != null) {
            return method.getAnnotation(OptLog.class);
        }
        return null;
    }

    /**
     *
     * 获取注解中对方法的描述信息
     *
     * @param joinPoint joinPoint
     * @param optLog operationLog
     * @param operationLog operationLogDTO
     *
     * @author liuchink
     * @since 2022/7/6 14:38
     */
    public void getControllerMethodDescription(JoinPoint joinPoint, OptLog optLog, OperationLog operationLog) {
        // 设置action动作
        operationLog.setBusinessType(optLog.businessType().getValue());
        // 设置标题
        operationLog.setTitle(optLog.title());
        // 设置操作人类别
        operationLog.setOperType(optLog.operatorType().getValue());
        // 是否需要保存request，参数和值
        if (optLog.isSaveRequestData()) {
            // 获取参数的信息，传入到数据库中。
            setRequestValue(joinPoint, operationLog);
        }
    }

    /**
     *
     * 获取请求的参数
     *
     * @param joinPoint joinPoint
     *
     * @author liuchink
     * @since 2022/7/6 14:40
     */
    private void setRequestValue(JoinPoint joinPoint, OperationLog operationLog) {
        String requestMethod = operationLog.getRequestMethod();
        if (HttpMethod.PUT.name().equals(requestMethod) || HttpMethod.POST.name().equals(requestMethod)) {
            String params = argsArrayToString(joinPoint.getArgs());
            // 请求参数截取字符
            operationLog.setOperParam(StringUtils.substring(params, 0, 3500));
        }
    }

    /**
     *
     * 参数拼装
     *
     * @param paramsArray paramsArray
     *
     * @return java.lang.String
     * @author liuchink
     * @since 2022/7/6 14:46
     */
    private String argsArrayToString(Object[] paramsArray) {
        StringBuilder params = new StringBuilder();
        if (paramsArray != null) {
            for (Object o : paramsArray) {
                if (!isFilterObject(o)) {
                    Object jsonObj = JSON.toJSON(o);
                    if (CharSequenceUtil.startWith(jsonObj.toString(), JwtConstant.JWT_SEPARATOR)) {
                        continue;
                    }
                    params.append(jsonObj).append("|");
                }
            }
        }
        return params.toString().trim();
    }

    /**
     *
     * 判断是否需要过滤的对象
     *
     * @param o o
     *
     * @return boolean
     * @author liuchink
     * @since 2022/7/6 14:45
     */
    @SuppressWarnings("rawtypes")
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection collection = (Collection)o;
            for (Object value : collection) {
                if (value instanceof MultipartFile) {
                    break;
                }
            }
        } else if (Map.class.isAssignableFrom(clazz)) {
            Map map = (Map)o;
            for (Object value : map.entrySet()) {
                Map.Entry entry = (Map.Entry)value;
                if (entry.getValue() instanceof MultipartFile) {
                    break;
                }
            }
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse;
    }

    private OptLogUser getOptLogUser(OptLogBusinessType optLogBusinessType, String userId) {
        OptLogUser optLogUser = null;
        switch (optLogBusinessType) {
            case MANAGE:
                optLogUser = JSON.parseObject(
                    redisTemplate.opsForValue().get(SystemConstant.ADMIN_LOGIN_ACCESS_TOKEN_KEY + userId),
                    OptLogUser.class);
                break;
            case APPLET:
                optLogUser = JSON.parseObject(
                    redisTemplate.opsForValue().get(SystemConstant.APPLET_LOGIN_ACCESS_TOKEN_KEY + userId),
                    OptLogUser.class);
                break;
            case APP:
                optLogUser = JSON.parseObject(
                    redisTemplate.opsForValue().get(SystemConstant.APP_LOGIN_ACCESS_TOKEN_KEY + userId),
                    OptLogUser.class);
                break;
            default:
        }
        return optLogUser;
    }

}
