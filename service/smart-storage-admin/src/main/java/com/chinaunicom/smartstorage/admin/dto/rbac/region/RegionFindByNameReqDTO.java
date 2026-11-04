package com.chinaunicom.smartstorage.admin.dto.rbac.region;



import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author liuchink
 * @since 2023/7/31 9:15
 */
@Data
public class RegionFindByNameReqDTO  {


    @Schema(description = "名称", nullable = true, example = "12345")
    @NotBlank(message = "名称不能为空")
    private String name;

}
