package com.chinaunicom.smartstorage.common.optlog;

/**
 * 操作人类别
 *
 * @author liuchink
 * @since 2022/7/6 14:12
 */
public enum OptLogOperatorType {

    /**
     * 其它
     */
    OTHER("OTHER"),

    /**
     * 新增
     */
    INSERT("INSERT"),

    /**
     * 修改
     */
    UPDATE("UPDATE"),

    /**
     * 删除
     */
    DELETE("DELETE"),

    /**
     * 授权
     */
    GRANT("GRANT"),

    /**
     * 导出
     */
    EXPORT("EXPORT"),

    /**
     * 导入
     */
    IMPORT("IMPORT"),

    /**
     * 强退
     */
    FORCE("FORCE"),

    /**
     * 生成代码
     */
    GENCODE("GENCODE"),

    /**
     * 清空数据
     */
    CLEAN("CLEAN"),
    /**
     * 查询
     */
    FIND("FIND");

    private final String value;

    OptLogOperatorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
