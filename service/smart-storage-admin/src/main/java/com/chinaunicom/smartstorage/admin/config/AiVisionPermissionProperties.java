package com.chinaunicom.smartstorage.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 视觉类功能的权限配置（多个子功能共用一份）。
 *
 * <p><b>为什么用数字权限 ID 而不是权限码字符串</b>：本项目的授权模型里，
 * {@code base_rights_info} 只有 {@code id} 与 {@code name} 两列，没有
 * {@code code}/{@code perms} 列；登录时 {@code TokenUserDetailsServiceImpl}
 * 把权限装配成 {@code new SimpleGrantedAuthority(Convert.toStr(right.getId()))}，
 * 即 Spring Security 的 authority 就是<b>数字权限 ID 的字符串形式</b>
 * （现网实例：{@code 10101=添加}、{@code 10203=删除}）。
 * 因此 {@code hasAuthority('videoOcclusion:event:export')} 这类写法
 * 在本项目中永远不可能命中，必须按数字 ID 比对。</p>
 *
 * <p><b>为什么是「功能 → 操作 → 权限 ID」两层</b>：每个子功能在目标环境里
 * 都会各自申请一组菜单与权限行，ID 互不相同。用两层结构可以让
 * {@code ai-vision.permission.rights.videoOcclusion.EXPORT} 这样的键
 * 自然绑定进 {@code Map<String, Map<String, String>>}，
 * 新增功能只加配置、不改代码。</p>
 *
 * <p><b>失败即拒绝</b>：{@code enabled=true} 但某个功能或操作没有配置权限 ID 时，
 * 该操作一律拒绝，不会退化成「仅校验已登录」。</p>
 *
 * <p><b>默认关闭</b>：{@code enabled=false} 时行为与接入权限前完全一致
 * （只要求已登录 + 数据范围下推），便于在权限行尚未入库的环境先行部署。</p>
 *
 * @author aiVision 共享基座
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai-vision.permission")
public class AiVisionPermissionProperties {

    /**
     * 是否启用功能级权限校验。
     * <p>开启前置条件：目标环境的 {@code base_rights_info} 已插入各功能的权限行，
     * 并已绑定到相应角色组，否则所有用户都会被拒绝。</p>
     */
    private boolean enabled = false;

    /**
     * 功能标识 → 操作键 → 数字权限 ID。
     * <p>操作键取值见各功能的 {@code {Feature}Constant.Permission}。</p>
     */
    private Map<String, Map<String, String>> rights = new LinkedHashMap<>();
}
