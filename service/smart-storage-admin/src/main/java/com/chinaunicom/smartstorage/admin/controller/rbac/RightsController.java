package com.chinaunicom.smartstorage.admin.controller.rbac;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinaunicom.smartstorage.common.controller.BaseController;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * 权限信息
 *
 * @author liuchink
 * @since 2020/4/8 21:22
 */
@Hidden
@Tag(name = "系统管理-权限管理")
@RestController
@RequestMapping("/rbac/rights")
@Slf4j
public class RightsController extends BaseController {
}
