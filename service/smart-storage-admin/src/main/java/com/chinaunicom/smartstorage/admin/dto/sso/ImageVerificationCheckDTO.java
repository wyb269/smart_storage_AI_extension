package com.chinaunicom.smartstorage.admin.dto.sso;



import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;


import lombok.Data;

/**
 *
 *
 * @author liuchink
 * @since 2022/5/9 13:03
 */
@Data
public class ImageVerificationCheckDTO  {


    /**
     * 滑动验证码，X轴
     */
    @NotNull(message = "参数内容能为空")
    @Schema(description = "X轴", nullable = true, example = "1")
    private Integer x;

    /**
     * 滑动验证码，Y轴
     */
    @NotNull(message = "参数内容能为空")
    @Schema(description = "Y轴", nullable = true, example = "1")
    private Integer y;

}