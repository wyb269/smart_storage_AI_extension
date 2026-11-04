package com.chinaunicom.smartstorage.admin.utils;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import lombok.Data;

/**
 *
 *
 * @author liuchink
 * @since 2022/5/9 13:22
 */
@Data
public class ImageRead {

	private BufferedImage image;

	private String fileExtension;

	private InputStream inputStream;

}
