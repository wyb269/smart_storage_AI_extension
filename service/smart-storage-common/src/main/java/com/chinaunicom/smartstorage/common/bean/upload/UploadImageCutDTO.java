package com.chinaunicom.smartstorage.common.bean.upload;



import lombok.Data;

/**
 * 图片裁切
 *
 * @author liuchink
 * @since 2019/6/2 14:37
 */
@Data
public class UploadImageCutDTO  {


    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
}
