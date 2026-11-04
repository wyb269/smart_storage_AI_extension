package com.chinaunicom.smartstorage.common.bean;



import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 条件查询基础类
 *
 * @author liuchink
 * @since 2017/12/18 10:57
 */
@Data
public class PageQueryDTO  {


    public PageQueryDTO() {
        this.pageNo = 1;
        this.pageSize = 15;
        this.pageTotal = true;
    }

    /**
     * 页数
     */
    @Schema(description = "pageNo", nullable = true, example = "1")
    private Integer pageNo;
    /**
     * 条数
     */
    @Schema(description = "pageSize", nullable = true, example = "10")
    private Integer pageSize;
    @Schema(description = "pageTotal", nullable = true, example = "true")
    private Boolean pageTotal;

}
