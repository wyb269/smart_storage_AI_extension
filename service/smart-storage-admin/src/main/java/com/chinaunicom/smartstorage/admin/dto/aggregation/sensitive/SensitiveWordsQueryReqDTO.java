package com.chinaunicom.smartstorage.admin.dto.aggregation.sensitive;

import com.chinaunicom.smartstorage.common.bean.BaseQueryDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class SensitiveWordsQueryReqDTO extends BaseQueryDTO {
    /**
     * 敏感词
     */
    @Schema(description = "敏感词", nullable = true, example = "12345")
    private String senWord;
}