package com.chinaunicom.smartstorage.applet.entity.aggregation.dict;


import java.util.List;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("base_dict_type")
public class DictType  {

    /**
     * 字典主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 字典名称
     */
    private String name;

    /**
     * 字典类型
     */
    private String type;

    /**
     * 状态1=启用、2=停用
     */

    private Integer showState;

    /**
     * 备注
     */
    private String remark;

    /**
     * 字典内容
     */
    @TableField(exist = false)
    private List<DictData> listDictData;
}
