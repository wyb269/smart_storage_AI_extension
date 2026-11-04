package com.chinaunicom.smartstorage.admin.entity.aggregation;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("base_sensitive_words")
@Data
public class SensitiveWords {
    /**
     * id
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 敏感词
     */
    private String senWord;

    /**
     * 替换词
     */
    private String repWord;

    /**
     * 备注
     */
    private String remark;

    /**
     *
     */
    private String addUser;

    /**
     *
     */
    private LocalDateTime addTime;
    /**
     *
     */
    private String updateUser;

    /**
     *
     */
    private LocalDateTime updateTime;
}