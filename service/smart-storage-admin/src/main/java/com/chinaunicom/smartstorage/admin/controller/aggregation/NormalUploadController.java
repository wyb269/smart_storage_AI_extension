package com.chinaunicom.smartstorage.admin.controller.aggregation;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.chinaunicom.smartstorage.admin.config.PathConfiguration;
import com.chinaunicom.smartstorage.common.bean.upload.UploadFile;
import com.chinaunicom.smartstorage.common.bean.upload.UploadFileRespDTO;
import com.chinaunicom.smartstorage.common.bean.upload.UploadImage;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.liuchink.chinkutils.date.ChinkDateFormatUtils;
import com.liuchink.chinkutils.date.ChinkDateValueUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * 文件上传
 *
 * @author liuchink 2017/12/18 10:41
 */
@Tag(name = "系统管理-文件上传")
@CrossOrigin
@RestController
@RequestMapping("/aggregation/normalUpload")
@Slf4j
public class NormalUploadController extends BaseController {
    @Resource
    private PathConfiguration pathConfiguration;

    @Resource
    private MapperFacade mapperFacade;

    /**
     * 文件上传方法
     *
     * @param multipartFile multipartFile
     * @author liuchink
     * @since 2017/5/28 23:29
     */
    @Operation(summary = "文件上传")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping("uploadFileAction")
    public ActionResult<UploadFileRespDTO, String> uploadFile(@RequestPart("file") MultipartFile multipartFile) {
        ActionResult<UploadFileRespDTO, String> actionResult = new ActionResult<>();
        UploadFile uploadFile = new UploadFile();
        // 文件后缀
        String fileMark = StringUtils.substring(multipartFile.getOriginalFilename(),
            StrUtil.lastIndexOf(multipartFile.getOriginalFilename(), ".", 0, true));
        // 文件名称
        String fileName = Convert.toStr(IdUtil.simpleUUID());
        // 文件目录
        String fileDir = ChinkDateValueUtils.getNowDateTime().toString(ChinkDateFormatUtils.DATEFORMAT_YYYYMMDD);
        // 文件磁盘
        String fileDisk = pathConfiguration.getUploadPathDisk();
        // 文件全路径
        String filePath = fileDisk + fileDir + File.separator + fileName + fileMark;
        uploadFile.setFileDir(fileDir);
        uploadFile.setFileDisk(fileDisk);
        uploadFile.setFileMark(fileMark);
        uploadFile.setFileName(fileName);
        uploadFile.setFilePath(filePath);
        uploadFile.setFileOriginalName(multipartFile.getOriginalFilename());
        uploadFile.setFileDirAndName(uploadFile.getFileDir() + File.separator + uploadFile.getFileName());
        uploadFile.setFileWebPath(uploadFile.getFileDir() + "/" + uploadFile.getFileName() + fileMark);
        UploadFileRespDTO uploadFileRespDTO = mapperFacade.map(uploadFile, UploadFileRespDTO.class);
        File saveFile = new File(filePath);
        if (!saveFile.getParentFile().exists() && !saveFile.getParentFile().mkdirs()) {
            throw new CustomException();
        }
        try (InputStream inputStream = multipartFile.getInputStream();
            OutputStream outputStream = Files.newOutputStream(saveFile.toPath())) {
            IoUtil.copy(inputStream, outputStream);
            actionResult.getStatus().setSuccess(true);
            actionResult.setResult(uploadFileRespDTO);
            return actionResult;
        } catch (IllegalStateException | IOException e) {
            throw new CustomException(e);
        }
    }

    /**
     * 图片上传
     *
     * @param multipartFile multipartFile
     * @param imageParams imageParams
     * @author liuchink
     * @since 2019/6/4 16:25
     */
    @Operation(summary = "图片上传")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping("uploadImageFileAction")
    public ActionResult<UploadFileRespDTO, String> uploadImageFile(

        @RequestPart("file") MultipartFile multipartFile, @RequestParam("imageParams") String imageParams) {
        UploadImage uploadImage = new UploadImage();
        if (CharSequenceUtil.isNotBlank(imageParams)) {
            uploadImage = JSON.parseObject(imageParams, UploadImage.class);
        }
        ActionResult<UploadFileRespDTO, String> actionResult = new ActionResult<>();
        UploadFile uploadFile = new UploadFile();

        // 文件后缀
        String fileMark = StringUtils.substring(multipartFile.getOriginalFilename(),
            StrUtil.lastIndexOf(multipartFile.getOriginalFilename(), ".", 0, true));
        // 文件名称
        String fileName = Convert.toStr(IdUtil.simpleUUID());
        // 文件目录
        String fileDir = ChinkDateValueUtils.getNowDateTime().toString(ChinkDateFormatUtils.DATEFORMAT_YYYYMMDD);
        // 文件磁盘
        String fileDisk = pathConfiguration.getUploadPathDisk();
        // 文件全路径
        String filePath = fileDisk + fileDir + File.separator + fileName + fileMark;
        uploadFile.setFileDir(fileDir);
        uploadFile.setFileDisk(fileDisk);
        uploadFile.setFileMark(fileMark);
        uploadFile.setFileName(fileName);
        uploadFile.setFilePath(filePath);
        uploadFile.setFileOriginalName(multipartFile.getOriginalFilename());
        uploadFile.setFileDirAndName(uploadFile.getFileDir() + File.separator + uploadFile.getFileName());
        uploadFile.setFileWebPath(uploadFile.getFileDir() + "/" + uploadFile.getFileName() + fileMark);
        UploadFileRespDTO uploadFileRespDTO = mapperFacade.map(uploadFile, UploadFileRespDTO.class);
        File saveFile = new File(filePath);
        if (!saveFile.getParentFile().exists() && !saveFile.getParentFile().mkdirs()) {
            throw new CustomException();
        }
        try {
            Image image = ImgUtil.read(multipartFile.getInputStream());
            // 图片按比例缩放
            if (uploadImage.getUploadImageZoomScale() != null) {
                image = ImgUtil.scale(image, uploadImage.getUploadImageZoomScale().getScale());
            }
            // 图片按尺寸缩放
            if (uploadImage.getUploadImageZoomSize() != null) {
                image = ImgUtil.scale(image, uploadImage.getUploadImageZoomSize().getWidth(),
                    uploadImage.getUploadImageZoomSize().getHeight(), Color.lightGray);
            }
            // 图片裁剪
            if (uploadImage.getUploadImageCut() != null) {
                Rectangle rectangle =
                    new Rectangle(uploadImage.getUploadImageCut().getX(), uploadImage.getUploadImageCut().getY(),
                        uploadImage.getUploadImageCut().getWidth(), uploadImage.getUploadImageCut().getHeight());
                image = ImgUtil.cut(image, rectangle);
            }
            // 图片文字水印
            if (uploadImage.getUploadImagePressText() != null) {
                Font font = new Font("宋体", Font.BOLD, 40);
                image = ImgUtil.pressText(image, uploadImage.getUploadImagePressText().getPressText(), Color.lightGray,
                    font, uploadImage.getUploadImagePressText().getX(), uploadImage.getUploadImagePressText().getY(),
                    uploadImage.getUploadImagePressText().getAlpha());
            }
            // 图片图片水印
            if (uploadImage.getUploadImagePressImage() != null) {
                Image pressImage = ImgUtil.read(new File("c://a.jgp"));
                image = ImgUtil.pressImage(image, pressImage, uploadImage.getUploadImagePressImage().getX(),
                    uploadImage.getUploadImagePressImage().getY(), uploadImage.getUploadImagePressImage().getAlpha());
            }
            ImgUtil.write(image, saveFile);
            actionResult.getStatus().setSuccess(true);
            actionResult.setResult(uploadFileRespDTO);
            return actionResult;
        } catch (IllegalStateException | IOException e) {
            throw new CustomException(e);
        }
    }
}
