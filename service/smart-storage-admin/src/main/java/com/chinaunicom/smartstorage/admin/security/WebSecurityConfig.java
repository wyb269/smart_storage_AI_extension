package com.chinaunicom.smartstorage.admin.security;

import cn.hutool.core.util.ArrayUtil;
import com.google.common.collect.Lists;
import com.chinaunicom.smartstorage.admin.config.SecurityIgnoreConfiguration;
import com.chinaunicom.smartstorage.admin.constant.TokenWhiteUrlConstant;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;
import java.util.Set;

/**
 * security配置文件
 *
 * @author liuchink
 * @since 2020/6/9 9:31
 */
@Configuration
// @EnableWebSecurity
// @EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@EnableMethodSecurity
public class WebSecurityConfig {
    @Resource
    private MyAuthenticationEntryPoint myAuthenticationEntryPoint;
    @Resource
    private MyAccessDeineHandler myAccessDeineHandler;
    @Resource
    private JwtTokenFilter jwtTokenFilter;
    @Resource
    private SecurityIgnoreConfiguration securityIgnoreConfiguration;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        // 允许访问
        List<String> listAllowedUrl = Lists.newArrayList();
        listAllowedUrl.addAll(TokenWhiteUrlConstant.TOKEN_PASS_PATH);
        listAllowedUrl.addAll(TokenWhiteUrlConstant.KNIFE4J_PATH);
        final Set<String> initSecurityIgonre = securityIgnoreConfiguration.initSecurityIgnore();
        listAllowedUrl.addAll(initSecurityIgonre);
        httpSecurity.authorizeHttpRequests(matcherRegistry -> {
            matcherRegistry.requestMatchers(allowedUrl(listAllowedUrl)).permitAll();
            matcherRegistry.anyRequest().authenticated();
        });
        httpSecurity.csrf(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable).logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.NEVER));
        httpSecurity
            .exceptionHandling(httpSecurityExceptionHandlingConfigurer -> httpSecurityExceptionHandlingConfigurer
                .accessDeniedHandler(myAccessDeineHandler).authenticationEntryPoint(myAuthenticationEntryPoint));
        httpSecurity.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new UserLoginPasswordEncoder();
    }

    private RequestMatcher[] allowedUrl(List<String> listAllowedUrl) {
        List<RequestMatcher> listAntPathRequestMatcher = Lists.newArrayList();
        for (String url : listAllowedUrl) {
            listAntPathRequestMatcher.add(AntPathRequestMatcher.antMatcher(url));
        }
        return ArrayUtil.toArray(listAntPathRequestMatcher, RequestMatcher.class);
    }
}
