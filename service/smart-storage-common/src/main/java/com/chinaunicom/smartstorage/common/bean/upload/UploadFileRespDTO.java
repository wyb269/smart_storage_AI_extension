package com.chinaunicom.smartstorage.common.bean.upload;



import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件上传对象
 *
 * @author liuchink
 * @since 2019/6/1 15:21
 */
@Data
public class UploadFileRespDTO  {
    @Schema(description = "fileDisk", nullable = true, example = "12345")
    private String fileDisk;
    @Schema(description = "fileDir", nullable = true, example = "12345")
    private String fileDir;
    @Schema(description = "filePath", nullable = true, example = "12345")
    private String filePath;
    @Schema(description = "fileWebPath", nullable = true, example = "12345")
    private String fileWebPath;
    @Schema(description = "fileName", nullable = true, example = "12345")
    private String fileName;
    @Schema(description = "fileDirAndName", nullable = true, example = "12345")
    private String fileDirAndName;
    @Schema(description = "fileMark", nullable = true, example = "12345")
    private String fileMark;
    @Schema(description = "fileOriginalName", nullable = true, example = "12345")
    private String fileOriginalName;
}
