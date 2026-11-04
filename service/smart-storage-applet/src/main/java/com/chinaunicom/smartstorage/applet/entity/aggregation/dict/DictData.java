package com.chinaunicom.smartstorage.applet.entity.aggregation.dict;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 字典
 *
 * @author liuchink
 * @since 2022/6/29 17:18
 */
@Data
@TableName("base_dict_data")
public class DictData {

    /**
     * 字典主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 字典排序
     */
    private Integer sort;

    /**
     * 字典标签
     */
    private String label;

    /**
     * 字典键值
     */
    private String description;

    /**
     * 字典类型
     */
    private String type;

    /**
     * 是否默认1=默认、2=非默认
     */

    private Integer isDefault;

    /**
     * 状态1=启用、2=停用
     */

    private Integer showState;

    /**
     * 备注
     */
    private String remark;
    private String cssClass;
    private String listClass;
}
