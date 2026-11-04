package com.chinaunicom.smartstorage.admin.dto.doorAnomaly;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 视频播放信息查询请求。
 *
 * <p><b>契约统一说明</b>：本功能全部接口采用 <b>POST + JSON 请求体</b>。
 * 此前 {@code getStreamInfo} 使用 {@code @RequestParam} 接收 {@code doorId}，
 * 而前端发送的是 JSON 请求体，导致参数绑定失败（第 21 节接口基线要求
 * 沿用项目 POST 查询风格）。本 DTO 用于把契约显式化为 {@code @RequestBody}，
 * 避免形成第三套契约。</p>
 *
 * <p><b>安全约束</b>：请求体只承载查询条件，
 * 不得携带任何用户身份或数据范围字段（由后端从登录态获取）。</p>
 *
 * @author 仓门异动功能
 */
@Data
@Schema(description = "视频播放信息查询请求")
public class StreamInfoReqDTO {

    @NotBlank(message = "仓门标识不能为空")
    @Schema(description = "仓门标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String doorId;
}