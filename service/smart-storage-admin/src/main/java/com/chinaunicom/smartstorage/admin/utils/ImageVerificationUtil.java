package com.chinaunicom.smartstorage.admin.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.chinaunicom.smartstorage.admin.dto.sso.ImageVerificationRespDTO;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;

import cn.hutool.core.codec.Base64Decoder;
import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * 图片验证工具
 *
 * @author liuchink
 * @since 2022/5/9 13:23
 */
@Slf4j
public class ImageVerificationUtil {

    /**
     * 默认图片宽度
     */
    private static final int DEFAULT_IMAGE_WIDTH = 370;

    /**
     * 默认图片高度
     */
    private static final int DEFAULT_IMAGE_HEIGHT = 129;

    /**
     *
     * 生成感兴趣区域坐标
     *
     * @param bufferedImage verificationImage
     * @param templateImage templateImage
     *
     * @return com.liuchink.chinkframe.cloud.feign.entity.dto.rbac.captcha.ImageVerificationRespDTO
     * @author liuchink
     * @since 2022/5/9 13:24
     */
    public static ImageVerificationRespDTO generateCutoutCoordinates(BufferedImage bufferedImage,
        BufferedImage templateImage) {

        int x;
        int y;
        ImageVerificationRespDTO imageVerificationRespDTO = new ImageVerificationRespDTO();

        // 原图宽度
        // int VERIFICATION_IMAGE_WIDTH = verificationImage.getWidth();
        // 原图高度
        // int VERIFICATION_IMAGE_HEIGHT = verificationImage.getHeight();
        // 抠图模板宽度
        int templateImageWidth = templateImage.getWidth();
        // 抠图模板高度
        int templateImageHeight = templateImage.getHeight();

        // 取范围内坐标数据，坐标抠图一定要落在原图中，否则会导致程序错误
        x = RandomUtil.randomInt(DEFAULT_IMAGE_WIDTH - templateImageWidth)
            % (DEFAULT_IMAGE_WIDTH - templateImageWidth - templateImageWidth + 1) + templateImageWidth;
        y = RandomUtil.randomInt(DEFAULT_IMAGE_HEIGHT - templateImageWidth)
            % (DEFAULT_IMAGE_HEIGHT - templateImageWidth - templateImageWidth + 1) + templateImageWidth;
        if (templateImageHeight - DEFAULT_IMAGE_HEIGHT >= 0) {
            y = RandomUtil.randomInt(10);
        }
        imageVerificationRespDTO.setX(x);
        imageVerificationRespDTO.setY(y);

        return imageVerificationRespDTO;
    }

    /**
     *
     * 根据模板图裁剪图片，生成源图遮罩图和裁剪图
     *
     * @param originImage originImage
     * @param inputStream inputStream
     * @param originImageFileType originImageFileType
     * @param templateImage templateImage
     * @param templateImageFileType templateImageFileType
     * @param x x
     * @param y y
     *
     * @return ImageVerificationRespDTO
     * @author liuchink
     * @since 2022/5/9 13:26
     */
    public static ImageVerificationRespDTO pictureTemplateCutout(BufferedImage originImage, InputStream inputStream,
        String originImageFileType, BufferedImage templateImage, String templateImageFileType, int x, int y) {
        ImageVerificationRespDTO imageVerificationRespDTO = new ImageVerificationRespDTO();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            int templateImageWidth = templateImage.getWidth();
            int templateImageHeight = templateImage.getHeight();

            // 切块图 根据模板图尺寸创建一张透明图片
            BufferedImage cutoutImage =
                new BufferedImage(templateImageWidth, templateImageHeight, templateImage.getType());

            // 根据坐标获取感兴趣区域
            BufferedImage interestArea =
                getInterestArea(x, y, templateImageWidth, templateImageHeight, inputStream, originImageFileType);

            // 根据模板图片切图
            cutoutImageByTemplateImage(interestArea, templateImage, cutoutImage);

            // 图片绘图
            int bold = 5;
            Graphics2D graphics = cutoutImage.createGraphics();
            graphics.setBackground(Color.white);

            // 设置抗锯齿属性
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setStroke(new BasicStroke(bold, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            graphics.drawImage(cutoutImage, 0, 0, null);
            graphics.dispose();

            // 原图生成遮罩
            BufferedImage shadeImage = generateShadeByTemplateImage(originImage, templateImage, x, y);

            // 图片转为二进制字符串
            ImageIO.write(originImage, originImageFileType, byteArrayOutputStream);
            byte[] originImageBytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.flush();
            byteArrayOutputStream.reset();
            // 图片加密成base64字符串
            String originImageString = Base64Encoder.encode(originImageBytes);
            imageVerificationRespDTO.setOriginImage(originImageString);

            ImageIO.write(shadeImage, templateImageFileType, byteArrayOutputStream);
            // 图片转为二进制字符串
            byte[] shadeImageBytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.flush();
            byteArrayOutputStream.reset();
            // 图片加密成base64字符串
            String shadeImageString = Base64Encoder.encode(shadeImageBytes);
            imageVerificationRespDTO.setShadeImage(shadeImageString);

            ImageIO.write(cutoutImage, templateImageFileType, byteArrayOutputStream);
            // 图片转为二进制字符串
            byte[] cutoutImageBytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.reset();
            // 图片加密成base64字符串
            String cutoutImageString = Base64Encoder.encode(cutoutImageBytes);
            imageVerificationRespDTO.setCutoutImage(cutoutImageString);

        } catch (IOException e) {
            throw new CustomException(e);
        }
        return imageVerificationRespDTO;
    }

    /**
     * 根据模板图生成遮罩图
     *
     * @param originImage originImage
     * @param templateImage templateImage
     * @param x x
     * @param y y
     * @return java.awt.image.BufferedImage
     * @author liuchink
     * @since 2022/5/9 13:31
     */
    private static BufferedImage generateShadeByTemplateImage(BufferedImage originImage, BufferedImage templateImage,
        int x, int y) {
        // 根据原图，创建支持alpha通道的rgb图片
        BufferedImage shadeImage =
            new BufferedImage(originImage.getWidth(), originImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        // 原图片矩阵
        int[][] originImageMatrix = getMatrix(originImage);
        // 模板图片矩阵
        int[][] templateImageMatrix = getMatrix(templateImage);

        // 将原图的像素拷贝到遮罩图
        for (int i = 0; i < originImageMatrix.length; i++) {
            for (int j = 0; j < originImageMatrix[0].length; j++) {
                int rgb = originImage.getRGB(i, j);
                // 获取rgb色度
                int r = (0xff & rgb);
                int g = (0xff & (rgb >> 8));
                int b = (0xff & (rgb >> 16));
                // 无透明处理
                rgb = r + (g << 8) + (b << 16) + (255 << 24);
                shadeImage.setRGB(i, j, rgb);
            }
        }

        // 对遮罩图根据模板像素进行处理
        for (int i = 0; i < templateImageMatrix.length; i++) {
            for (int j = 0; j < templateImageMatrix[0].length; j++) {
                int rgb = templateImage.getRGB(i, j);

                // 对源文件备份图像(x+i,y+j)坐标点进行透明处理
                if (rgb < 0) {
                    int originRgb = shadeImage.getRGB(x + i, y + j);
                    int r = (0xff & originRgb);
                    int g = (0xff & (originRgb >> 8));
                    int b = (0xff & (originRgb >> 16));

                    originRgb = r + (g << 8) + (b << 16) + (140 << 24);

                    // 对遮罩透明处理
                    shadeImage.setRGB(x + i, y + j, originRgb);
                    // 设置遮罩颜色
                    // shadeImage.setRGB(x + i, y + j, originRGB);

                }

            }
        }

        return shadeImage;
    }

    /**
     * 根据模板图抠图
     *
     * @param interestArea interestArea
     * @param cutoutImage cutoutImage
     * @param templateImage templateImage
     * @author liuchink
     * @since 2022/5/9 13:33
     */
    private static void cutoutImageByTemplateImage(BufferedImage interestArea, BufferedImage templateImage,
        BufferedImage cutoutImage) {
        // 获取兴趣区域图片矩阵
        int[][] interestAreaMatrix = getMatrix(interestArea);
        // 获取模板图片矩阵
        int[][] templateImageMatrix = getMatrix(templateImage);

        // 将模板图非透明像素设置到剪切图中
        for (int i = 0; i < templateImageMatrix.length; i++) {
            for (int j = 0; j < templateImageMatrix[0].length; j++) {
                int rgb = templateImageMatrix[i][j];
                if (rgb < 0) {
                    cutoutImage.setRGB(i, j, interestArea.getRGB(i, j));
                }
            }
        }

    }

    /**
     *
     * 图片生成图像矩阵
     *
     * @param bufferedImage bufferedImage
     *
     * @return int[][]
     * @author liuchink
     * @since 2022/5/9 13:34
     */
    private static int[][] getMatrix(BufferedImage bufferedImage) {
        int[][] matrix = new int[bufferedImage.getWidth()][bufferedImage.getHeight()];
        for (int i = 0; i < bufferedImage.getWidth(); i++) {
            for (int j = 0; j < bufferedImage.getHeight(); j++) {
                matrix[i][j] = bufferedImage.getRGB(i, j);
            }
        }
        return matrix;
    }

    /**
     *
     * 获取感兴趣区域
     *
     * @param x x
     * @param inputStream inputStream
     * @param originImageType originImageType
     * @param templateImageHeight templateImageHeight
     * @param templateImageWidth templateImageWidth
     * @param y y
     * @return java.awt.image.BufferedImage
     * @author liuchink
     * @since 2022/5/9 13:34
     */
    private static BufferedImage getInterestArea(int x, int y, int templateImageWidth, int templateImageHeight,
        InputStream inputStream, String originImageType) {
        ImageReader imageReader = null;
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
            Iterator<ImageReader> imageReaderIterator = ImageIO.getImageReadersBySuffix(originImageType);
            if (!imageReaderIterator.hasNext()) {
                throw new CustomException("找不到支持该格式的图片解码器");
            }
            imageReader = imageReaderIterator.next();
            imageReader.setInput(imageInputStream, true);
            ImageReadParam imageReadParam = imageReader.getDefaultReadParam();
            Rectangle rectangle = new Rectangle(x, y, templateImageWidth, templateImageHeight);
            imageReadParam.setSourceRegion(rectangle);

            return imageReader.read(0, imageReadParam);

        } catch (IOException e) {
            throw new CustomException(e);
        } finally {
            if (imageReader != null) {
                imageReader.dispose();
            }
        }
    }

    /**
     * 切块图描边
     *
     * @param borderImage borderImage
     * @param borderImageFileType borderImageFileType
     * @param imageVerificationRespDTO imageVerificationReqDTO
     * @author liuchink
     * @since 2022/5/9 13:40
     */
    public static void cutoutImageEdge(ImageVerificationRespDTO imageVerificationRespDTO, BufferedImage borderImage,
        String borderImageFileType) {
        String cutoutImageString = imageVerificationRespDTO.getCutoutImage();
        // 图片解密成二进制字符创
        byte[] bytes = Base64Decoder.decode(cutoutImageString);

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            // 读取图片
            BufferedImage cutoutImage = ImageIO.read(byteArrayInputStream);
            // 获取模板边框矩阵， 并进行颜色处理
            int[][] borderImageMatrix = getMatrix(borderImage);
            for (int i = 0; i < borderImageMatrix.length; i++) {
                for (int j = 0; j < borderImageMatrix[0].length; j++) {
                    int rgb = borderImage.getRGB(i, j);
                    if (rgb < 0) {
                        cutoutImage.setRGB(i, j, -7237488);
                    }
                }
            }
            ImageIO.write(cutoutImage, borderImageFileType, byteArrayOutputStream);
            // 新模板图描边处理后转成二进制字符串
            byte[] cutoutImageBytes = byteArrayOutputStream.toByteArray();
            // 二进制字符串加密成base64字符串
            String cutoutImageStr = Base64Encoder.encode(cutoutImageBytes);
            imageVerificationRespDTO.setCutoutImage(cutoutImageStr);
        } catch (IOException e) {
            throw new CustomException(e);
        }
    }
}