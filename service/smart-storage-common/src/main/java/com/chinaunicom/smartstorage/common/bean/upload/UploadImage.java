package com.chinaunicom.smartstorage.common.bean.upload;


import com.alibaba.fastjson2.annotation.JSONField;

import lombok.Data;

/**
 * 图片属性
 *
 * @author liuchink
 * @since 2019/6/2 14:19
 */
@Data
public class UploadImage  {


    @JSONField(name = "uploadImageCut")
    private UploadImageCut uploadImageCut;
    @JSONField(name = "uploadImagePressText")
    private UploadImagePressText uploadImagePressText;
    @JSONField(name = "uploadImagePressImage")
    private UploadImagePressImage uploadImagePressImage;
    @JSONField(name = "uploadImageZoomScale")
    private UploadImageZoomScale uploadImageZoomScale;
    @JSONField(name = "uploadImageZoomSize")
    private UploadImageZoomSize uploadImageZoomSize;
}
