package com.chinaunicom.smartstorage.applet.dto.sso;



import com.alibaba.fastjson2.annotation.JSONField;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 *
 * @author liuchink
 * @since 2022/5/9 13:03
 */
@Data
public class ImageVerificationRespDTO  {
    /**
     * 滑动验证码，源图
     */
    @Schema(description = "源图", nullable = true, example = "12345")
    @JSONField(serialize = false)
    private String originImage;
    /**
     * 滑动验证码，遮罩图
     */
    @Schema(description = "遮罩图", nullable = true, example = "12345")
    private String shadeImage;

    /**
     * 滑动验证码，裁剪图
     */
    @Schema(description = "裁剪图", nullable = true, example = "12345")
    private String cutoutImage;

    /**
     * 滑动验证码，X轴
     */
    @Schema(description = "X轴", nullable = true, example = "12345")
    private Integer x;

    /**
     * 滑动验证码，Y轴
     */
    @Schema(description = "Y轴", nullable = true, example = "12345")
    private Integer y;

}