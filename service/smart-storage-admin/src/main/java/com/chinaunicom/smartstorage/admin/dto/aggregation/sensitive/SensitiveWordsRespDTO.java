package com.chinaunicom.smartstorage.admin.dto.aggregation.sensitive;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SensitiveWordsRespDTO {
    /**
     * id
     */
    @Schema(description = "id", nullable = true, example = "12345")
    private String id;

    /**
     * 敏感词
     */
    @Schema(description = "敏感词", nullable = true, example = "12345")
    private String senWord;

    /**
     * 替换词
     */
    @Schema(description = "替换词", nullable = true, example = "12345")
    private String repWord;

    /**
     * 备注
     */
    @Schema(description = "备注", nullable = true, example = "12345")
    private String remark;

    /**
     *
     */
    // @Schema(description = "添加人", nullable = true, example = "12345")
    // private String addUser;

    /**
     *
     */
    @Schema(description = "添加时间", nullable = true, example = "12345")
    private LocalDateTime addTime;
    /**
     *
     */
    // @Schema(description = "修改人", nullable = true, example = "12345")
    // private String updateUser;

    /**
     *
     */
    @Schema(description = "修改时间", nullable = true, example = "12345")
    private LocalDateTime updateTime;
}