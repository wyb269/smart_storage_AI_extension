package com.chinaunicom.chinkframe.admin.test;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;

/**
 * RSA加密测试类
 * 用于测试使用RSA公钥加密密码
 *
 * 使用方法：
 * 1. 从数据库 base_system_config 表获取 rsaPublicKey、rsaPrivateKey 和 md5Solt 的值
 * 2. 替换下面常量中的公钥、私钥和盐值
 * 3. 修改待加密的密码
 * 4. 运行 main 方法
 *
 * @author system
 * @Date 2026-05-20
 */
public class RsaEncryptTest {

    // ============================================
    // TODO: 请从数据库 base_system_config 表中获取实际值
    // 获取SQL:
    // SELECT syc_value FROM base_system_config WHERE syc_group = 'rsa' AND syc_key = 'rsaPublicKey';
    // SELECT syc_value FROM base_system_config WHERE syc_group = 'rsa' AND syc_key = 'rsaPrivateKey';
    // SELECT syc_value FROM base_system_config WHERE syc_group = 'md5' AND syc_key = 'md5Solt';
    // ============================================

    /**
     * RSA公钥 - 用于前端加密
     */
    private static final String RSA_PUBLIC_KEY = System.getenv("SMART_STORAGE_TEST_RSA_PUBLIC_KEY");

    /**
     * RSA私钥 - 用于后端解密
     */
    private static final String RSA_PRIVATE_KEY = System.getenv("SMART_STORAGE_TEST_RSA_PRIVATE_KEY");

    /**
     * MD5盐值 - 用于密码加密
     */
    private static final String MD5_SOLT = System.getenv("SMART_STORAGE_TEST_MD5_SALT");

    // ============================================
    // 【解密功能】请将RSA加密后的Base64密文粘贴到这里
    // 注意：这里填入的是RSA公钥加密后的Base64密文，
    //       不是数据库中login_pwd字段的MD5值！
    //       （MD5是单向哈希，无法逆向解密）
    // 如果不需要解密，请将下面设为 null 或空字符串
    // ============================================
    private static final String ENCRYPTED_PASSWORD_TO_DECRYPT = null;

    public static void main(String[] args) {
        // ==================== 模式A：解密RSA加密的密码 ====================
        if (ENCRYPTED_PASSWORD_TO_DECRYPT != null && !ENCRYPTED_PASSWORD_TO_DECRYPT.isEmpty()) {
            System.out.println("========================================");
            System.out.println("【RSA解密模式】");
            System.out.println("========================================");
            try {
                // 使用私钥解密RSA加密的Base64密文，得到原始密码
                String originalPassword = decryptByPrivateKey(ENCRYPTED_PASSWORD_TO_DECRYPT, RSA_PRIVATE_KEY);
                System.out.println("RSA加密密文: " + ENCRYPTED_PASSWORD_TO_DECRYPT);
                System.out.println("解密后原始密码: " + originalPassword);

                // 同时展示这个原始密码存入数据库后的MD5值
                String dbPassword = SecureUtil.md5(MD5_SOLT + originalPassword);
                System.out.println("对应数据库login_pwd值: " + dbPassword);
                System.out.println("========================================");
            } catch (Exception e) {
                System.err.println("RSA解密失败: " + e.getMessage());
                System.err.println("请确认填入的是RSA公钥加密后的Base64密文，而不是数据库中的MD5值");
                e.printStackTrace();
            }
            return;
        }

        // ==================== 模式B：正向加密流程演示 ====================
        // 待加密的密码
//        String password = "change-me";
        String password = "change-me";

        try {
            // 1. 使用公钥加密（前端传输时使用）
            String encryptedPassword = encryptByPublicKey(password, RSA_PUBLIC_KEY);
            System.out.println("========================================");
            System.out.println("【第1步】原始密码: " + password);
            System.out.println("【第2步】RSA公钥加密后: " + encryptedPassword);
            System.out.println("========================================");

            // 2. 使用私钥解密（后端接收时使用）
            String decryptedPassword = decryptByPrivateKey(encryptedPassword, RSA_PRIVATE_KEY);
            System.out.println("【第3步】RSA私钥解密后: " + decryptedPassword);
            System.out.println("========================================");

            // 3. 计算最终存入数据库的密码（带盐值的MD5加密）
            // 公式: MD5(MD5盐值 + 解密后的密码)
            String finalPassword = SecureUtil.md5(MD5_SOLT + decryptedPassword);
            System.out.println("【第4步】存入数据库的最终密码: " + finalPassword);
            System.out.println("========================================");

            // 验证：用同样的方式计算，看结果是否一致
            String verifyPassword = encryptPassword(password);
            System.out.println("验证结果: " + (finalPassword.equals(verifyPassword) ? "成功 ✓" : "失败 ✗"));
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("加密/解密失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 完整的密码加密流程（与后端 PwdComponent.getRsaDecryptAndEncryptMd5 逻辑一致）
     * 1. RSA公钥加密
     * 2. RSA私钥解密
     * 3. MD5(盐值 + 解密后的密码)
     *
     * @param password 原始密码
     * @return 最终存入数据库的密码
     */
    public static String encryptPassword(String password) {
        // 1. RSA公钥加密
        String rsaEncrypted = encryptByPublicKey(password, RSA_PUBLIC_KEY);
        // 2. RSA私钥解密
        String rsaDecrypted = decryptByPrivateKey(rsaEncrypted, RSA_PRIVATE_KEY);
        // 3. MD5(盐值 + 解密后的密码)
        return SecureUtil.md5(MD5_SOLT + rsaDecrypted);
    }

    /**
     * 使用公钥加密
     *
     * @param content   待加密内容
     * @param publicKey RSA公钥
     * @return Base64编码的加密结果
     */
    public static String encryptByPublicKey(String content, String publicKey) {
        RSA rsa = SecureUtil.rsa(null, publicKey);
        return rsa.encryptBase64(content, KeyType.PublicKey);
    }

    /**
     * 使用私钥解密
     *
     * @param encryptedContent Base64编码的加密内容
     * @param privateKey       RSA私钥
     * @return 解密后的字符串
     */
    public static String decryptByPrivateKey(String encryptedContent, String privateKey) {
        RSA rsa = SecureUtil.rsa(privateKey, null);
        return rsa.decryptStr(encryptedContent, KeyType.PrivateKey);
    }
}
