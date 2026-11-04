package com.chinaunicom.smartstorage.admin.component;

import com.chinaunicom.smartstorage.admin.config.AiVisionPermissionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * AI 视觉类功能的权限判定入口，供 {@code @PreAuthorize} 以 SpEL bean 引用调用。
 *
 * <p>Bean 名固定为 {@code aiVisionPermission}，控制器写法：
 * {@code @PreAuthorize("@aiVisionPermission.has('videoOcclusion','EXPORT')")}。
 * 判定逻辑集中在这里而不是散落在各表达式中，原因有两点：
 * 权限 ID 是配置项、需要在「未配置」时统一按失败即拒绝处理；
 * 以及需要保留 {@code enabled=false} 的灰度开关。</p>
 *
 * <p><b>多个子功能共用本类</b>：第一个参数是功能标识，第二个是操作键，
 * 对应配置 {@code ai-vision.permission.rights.<功能>.<操作>}。</p>
 *
 * @author aiVision 共享基座
 */
@Slf4j
@Component("aiVisionPermission")
public class AiVisionPermissionChecker {

    private final AiVisionPermissionProperties properties;

    public AiVisionPermissionChecker(AiVisionPermissionProperties properties) {
        this.properties = properties;
    }

    /**
     * 当前登录用户是否具备某功能下某项操作的权限。
     *
     * @param feature   功能标识，如 {@code doorAnomaly}
     * @param operation 操作键，取自该功能的 {@code {Feature}Constant.Permission}
     * @return 未启用权限体系时恒为 true；启用时按数字权限 ID 精确比对
     */
    public boolean has(String feature, String operation) {
        if (!properties.isEnabled()) {
            return true;
        }
        Map<String, String> operations = properties.getRights().get(feature);
        String rightId = operations == null ? null : operations.get(operation);
        if (!StringUtils.hasText(rightId)) {
            // 启用却没配全：宁可拒绝，也不能悄悄退回「只要登录就放行」
            log.warn("[AiVision] 权限已启用但未配置 [{}:{}] 的权限ID，按拒绝处理", feature, operation);
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        boolean granted = authentication.getAuthorities().stream()
            .anyMatch(authority -> rightId.equals(authority.getAuthority()));
        if (!granted) {
            log.warn("[AiVision] 越权拒绝 feature={} operation={} requiredRightId={} loginId={}",
                feature, operation, rightId, authentication.getName());
        }
        return granted;
    }
}
