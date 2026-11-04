package com.chinaunicom.smartstorage.applet.controller.file;

import static org.bouncycastle.oer.its.Duration.minutes;
import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.applet.config.RustFSConfiguration;
import com.chinaunicom.smartstorage.common.bean.PresignRespDTO;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.enumbean.FileDomain;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Tag(name = "文件管理-S3签名")
@CrossOrigin
@RestController
@RequestMapping("/file/s3")
@Slf4j
public class S3Controller extends BaseController {


    @Resource
    private RustFSConfiguration rustFsConfiguration;

    @Resource
    private UidGenerator uidGenerator;

    @Resource
    private S3Presigner s3Presigner;

    @GetMapping("/presign-upload")
    @Operation(summary = "生成上传预签名 URL")
    public ActionResult<PresignRespDTO, String> presignUpload(
            @Schema(description = "业务域") @NotBlank(message = "业务域不能为空") @RequestParam String domain,
            @Schema(description = "文件名") @NotBlank(message = "文件名不能为空") @RequestParam String fileName) {

        // 安全校验 1、文件名合法性
        if (fileName.contains("..") || fileName.contains("/")) {
            return actionResultService.callBackResultFail("文件名不合法");
        }

        // 安全校验 2、文件扩展名
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1)
                .toLowerCase();
        if (!RustFSConfiguration.ALLOWED_EXTENSIONS.contains(ext)) {
            return actionResultService.callBackResultFail("文件扩展名不支持");
        }

        // 安全校验 3、业务域校验
        FileDomain fileDomain = FileDomain.fromCode(domain);

        // 生成对象Key
        String datePath = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String objectKey = fileDomain.getCode() + "/" + datePath + "/" + uidGenerator.getUID() + "_" + fileName;

        // 签名
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(rustFsConfiguration.getBucketName())
                .key(objectKey)
                //.contentLength(0L)
                //.contentMD5("")
                //.contentType(getMimeType(ext))
                .build();

        Duration duration = Duration.ofMinutes(Math.min(minutes, RustFSConfiguration.UPLOAD_MINUTES));

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putRequest)
                .signatureDuration(duration)
                .build();

        String presignedUrl = s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();
        return actionResultService.callBackResult(new PresignRespDTO(presignedUrl, fileName, objectKey));
    }


    @GetMapping("/presign-download")
    @Operation(summary = "生成下载预签名 URL")
    public ActionResult<String, String> presignDownload(
            @Schema(description = "对象") @NotBlank(message = "对象不能为空") @RequestParam String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(rustFsConfiguration.getBucketName())
                .key(objectKey)
                .build();

        Duration duration = Duration.ofMinutes(Math.min(minutes, RustFSConfiguration.DOWNLOAD_MINUTES));

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getObjectRequest)
                .build();

        String presignedUrl = s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
        return actionResultService.callBackResult(presignedUrl);
    }
}
