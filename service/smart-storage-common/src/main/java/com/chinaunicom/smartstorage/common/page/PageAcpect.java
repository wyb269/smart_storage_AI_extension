package com.chinaunicom.smartstorage.common.page;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ReflectUtil;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.chinaunicom.smartstorage.common.bean.PageQueryDTO;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.chinaunicom.smartstorage.common.result.QueryPageResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author liuchink
 * @since 2023/8/11 8:33
 */
@Aspect
@Component
@Order(4)
@Slf4j
public class PageAcpect {
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private MapperFacade mapperFacade;

    @Pointcut("@annotation(com.chinaunicom.smartstorage.common.page.QueryByPage)")
    public void annotationContent() {}

    @Around(value = "annotationContent()", argNames = "proceedingJoinPoint")
    public ActionResult<QueryPageResult<Object>, String> queryByPage(ProceedingJoinPoint proceedingJoinPoint) {
        int pageNo = 1;
        int pageSize = 15;
        boolean pageTotal = true;
        Object[] args = proceedingJoinPoint.getArgs();

        PageInfo<?> pageInfo;
        final QueryByPage annotationLog = getAnnotationLog(proceedingJoinPoint);
        QueryPageResult<Object> queryPageResult = new QueryPageResult<>();
        for (Object arg : args) {
            if (arg instanceof PageQueryDTO) {
                final PageQueryDTO pageQueryDTO = Convert.convert(PageQueryDTO.class, arg);
                pageNo = pageQueryDTO.getPageNo();
                pageSize = pageQueryDTO.getPageSize();
                pageTotal = pageQueryDTO.getPageTotal();
            }
        }
        try {
            if (pageNo == 0 && pageSize == 0) {
                PageMethod.startPage(pageNo, pageSize, false);
            } else {
                PageMethod.startPage(pageNo, pageSize, pageTotal);
            }
            final Object proceed = proceedingJoinPoint.proceed();
            if (proceed instanceof List) {
                pageInfo = new PageInfo<>((List<?>)proceed);
                if (CharSequenceUtil.isNotBlank(annotationLog.returnClass())) {
                    final List<?> list = mapperFacade.mapAsList(pageInfo.getList(),
                        ReflectUtil.newInstance(annotationLog.returnClass()).getClass());
                    queryPageResult.getData().addAll(list);
                } else {
                    queryPageResult.getData().addAll(pageInfo.getList());
                }
                if (pageNo == 0 && pageSize == 0) {
                    if (pageTotal) {
                        queryPageResult.setTotal(Convert.toLong(queryPageResult.getData().size()));
                    } else {
                        queryPageResult.setTotal(-1L);
                    }
                } else {
                    queryPageResult.setTotal(Convert.toLong(pageInfo.getTotal()));
                }

            }
        } catch (Throwable throwable) {
            log.error(SystemConstant.LOG_ERROR_TITLE, throwable.getMessage(), throwable);
            throw new CustomException(throwable.getMessage());
        } finally {
            PageMethod.clearPage();
        }
        return actionResultService.callBackResult(queryPageResult);
    }

    private QueryByPage getAnnotationLog(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature)signature;
        Method method = methodSignature.getMethod();
        return method.getAnnotation(QueryByPage.class);
    }
}
