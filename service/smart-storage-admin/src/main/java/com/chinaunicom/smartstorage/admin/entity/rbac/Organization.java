package com.chinaunicom.smartstorage.admin.entity.rbac;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组织机构表
 *
 * @author liuchink
 * @since 2020/4/5 12:45
 */
@Data
@TableName("base_organization_info")
public class Organization {

    /**
     * id
     */
    private String id;

    /**
     * 父级行政区划ID
     */
    private String parentId;
    @TableField(exist = false)
    private String parentName;

    /**
     * 名称
     */
    private String name;

    /**
     * 编码
     */
    // private String code;

    /**
     * 等级
     */
    private Integer levels;

    /**
     * 添加人
     */
    private String addUser;

    /**
     * 添加时间
     */
    private LocalDateTime addTime;

    /**
     * 修改人
     */
    private String updateUser;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 类型1=公司；2=仓库
     */
    private Integer type;

    /**
     * 统一编码
     */
    private String unifyEncoding;

    /**
     * 描述
     */
    private String description;

    /**
     * 企业性质
     */
    private String companyNature;

    /**
     * 工商登记注册号
     */
    private String businessRegistrationNumber;

    /**
     * 法人姓名
     */
    private String legalPersonName;

    /**
     * 法人电话
     */
    private String legalRepresentativePhoneNumber;

    /**
     * 省份
     */
    private String province;

    /**
     * 省份名称
     */
    private String provinceName;

    /**
     * 城市
     */
    private String city;

    /**
     * 城市名称
     */
    private String cityName;

    /**
     * 仓库性质
     */
    private String warehouseNature;

    /**
     * 库点设计储量
     */
    private String designStorage;

    /**
     * 库点设计面积
     */
    private String designArea;

    /**
     * 库区编码
     */
    private String libraryAreaCode;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;
    /**
     * 详细地址
     */
    private String detailedAddress;
}
