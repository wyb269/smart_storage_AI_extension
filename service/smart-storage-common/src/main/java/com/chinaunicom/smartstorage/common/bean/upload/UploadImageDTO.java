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
public class UploadImageDTO  {


    @JSONField(name = "uploadImageCut")
    private UploadImageCutDTO uploadImageCut;
    @JSONField(name = "uploadImagePressText")
    private UploadImagePressTextDTO uploadImagePressText;
    @JSONField(name = "uploadImagePressImage")
    private UploadImagePressImageDTO uploadImagePressImage;
    @JSONField(name = "uploadImageZoomScale")
    private UploadImageZoomScaleDTO uploadImageZoomScale;
    @JSONField(name = "uploadImageZoomSize")
    private UploadImageZoomSizeDTO uploadImageZoomSize;
}
