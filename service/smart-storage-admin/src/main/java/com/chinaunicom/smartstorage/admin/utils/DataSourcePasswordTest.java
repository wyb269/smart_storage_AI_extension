package com.chinaunicom.smartstorage.admin.utils;

import cn.hutool.crypto.SmUtil;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 数据源密码 SM4 加解密工具（与 HikariDataSourceConfiguration 保持一致）
 * <p>
 * 用法：
 * 1. 直接运行 main，按提示输入明文密码后回车（推荐，避免密码留在代码里）；
 * 2. 或在 IDEA 运行配置的 Program arguments 里传明文密码；
 * 3. 输出的大写 HEX 密文直接填到 application-xxx.yml 的 spring.datasource.password。
 * <p>
 * 临时工具类，用完可删除。
 *
 * @author liuchink
 */
public class DataSourcePasswordTest {

    public static void main(String[] args) {
        String plainPassword;
        if (args != null && args.length > 0) {
            plainPassword = args[0];
        } else {
            System.out.print("请输入数据库明文密码后回车: ");
            plainPassword = new Scanner(System.in).nextLine();
        }

        String encrypted = encrypt(plainPassword);
        System.out.println("密文(填到 application-dev.yml 的 spring.datasource.password) = " + encrypted);
        System.out.println("回验明文 = " + decrypt(encrypted));

        // 校验现有配置里的密文，确认密钥与算法一致
        System.out.println("配置密文校验: "
            + decrypt(System.getenv("SMART_STORAGE_TEST_ENCRYPTED_DATASOURCE_PASSWORD")));
    }

    /**
     * 加密：明文 -> HEX 密文（大写，和现有配置格式一致）
     */
    public static String encrypt(String plain) {
        return SmUtil.sm4(SystemConstant.DATASOURCE_SECRET_KEY.getBytes(StandardCharsets.UTF_8))
            .encryptHex(plain)
            .toUpperCase();
    }

    /**
     * 解密：HEX 密文 -> 明文
     */
    public static String decrypt(String encrypted) {
        return SmUtil.sm4(SystemConstant.DATASOURCE_SECRET_KEY.getBytes(StandardCharsets.UTF_8))
            .decryptStr(encrypted);
    }
}
