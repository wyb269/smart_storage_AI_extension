package com.chinaunicom.smartstorage.admin.dto.aggregation.sensitive;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SensitiveWordsUpdateReqDTO {
    /**
     * id
     */
    @Schema(description = "id", nullable = true, example = "12345")
    private String id;

    /**
     * 敏感词
     */
    @NotBlank(message = "敏感词不能为空")
    @Schema(description = "敏感词", nullable = true, example = "12345")
    private String senWord;

    /**
     * 替换词
     */
    @NotBlank(message = "替换词不能为空")
    @Schema(description = "替换词", nullable = true, example = "12345")
    private String repWord;

    /**
     * 备注
     */
    @Schema(description = "备注", nullable = true, example = "12345")
    private String remark;
}