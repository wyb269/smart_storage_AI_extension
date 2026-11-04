package com.chinaunicom.smartstorage.common.bean.upload;



import lombok.Data;

/**
 * 图片水印
 *
 * @author liuchink
 * @since 2019/6/2 14:37
 */
@Data
public class UploadImagePressImageDTO  {


    private Integer x;
    private Integer y;
    private Float alpha;
}
