package com.chinaunicom.smartstorage.admin.dto.doorAnomaly;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 事件人工处理请求。
 *
 * <p>对应 AI_HANDOFF.md 第 20.6 节「事件处理请求」。</p>
 *
 * <p><b>安全约束（关键）</b>：</p>
 * <ul>
 *   <li>本对象<b>不包含</b>处理人字段。处理人与处理时间
 *       必须由后端根据登录态写入，不信任前端传入（第 20.6、29.4 节）。</li>
 *   <li>eventVersion 为用户读取到的事件版本号，提交时必须原样回传。
 *       版本已变化时后端必须拒绝覆盖并提示「事件已被其他用户处理」。</li>
 * </ul>
 *
 * @author 仓门异动功能
 */
@Data
@Schema(description = "事件人工处理请求")
public class EventProcessReqDTO {

    @NotNull(message = "事件版本号不能为空")
    @Schema(description = "用户读取到的事件版本号，用于乐观锁并发控制",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer eventVersion;

    /**
     * 处理结果。
     *
     * <p><b>必须做白名单校验</b>：该值会原样写入
     * {@code ai_anomaly_event.process_status} 与审计记录的
     * {@code status_after}。只判空的话，客户端传 {@code PENDING}
     * 可以把已终态的事件重新打开，传任意其它字符串则会让该事件的
     * {@code ProcessStatusEnum.valueOf} 永久解析失败，
     * 事件被卡在「事件处理状态异常，请联系管理员」且代码里没有任何恢复路径。
     * {@code @Schema.allowableValues} 只是文档，不产生校验。</p>
     */
    @NotBlank(message = "处理结果不能为空")
    @Pattern(regexp = "VERIFIED_TRUE|VERIFIED_FALSE",
        message = "处理结果只能是 VERIFIED_TRUE（真实异常）或 VERIFIED_FALSE（误报）")
    @Schema(description = "处理结果 VERIFIED_TRUE 真实异常 / VERIFIED_FALSE 误报",
            requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"VERIFIED_TRUE", "VERIFIED_FALSE"})
    private String processResult;

    @NotBlank(message = "处理意见不能为空")
    @Size(max = 1000, message = "处理意见不能超过1000字")
    @Schema(description = "处理意见，按普通文本存储与输出，防止脚本注入",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String processOpinion;
}
