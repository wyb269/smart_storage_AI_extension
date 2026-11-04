package com.chinaunicom.smartstorage.admin.entity.rbac.role;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.chinaunicom.smartstorage.admin.entity.rbac.Organization;
import com.chinaunicom.smartstorage.admin.entity.rbac.Region;
import lombok.Data;
import org.apache.commons.compress.utils.Lists;

import java.time.LocalDateTime;
import java.util.List;

@TableName("base_role_group")
@Data
public class RoleGroup {
    /**
     * id
     */
    @TableId(type = IdType.INPUT)
    private String id;
    /**
     * 机构id
     */

    private String orgId;
    /**
     * 行政区ID
     */

    private String regionId;
    /**
     * 名称
     */
    private String name;

    /**
     * 备注
     */
    private String remark;

    /**
     * 添加人
     */

    private String addUser;
    /**
     * 修改人
     */

    private String updateUser;

    /**
     * 修改时间
     */

    private LocalDateTime updateTime;
    /**
     * 添加时间
     */

    private LocalDateTime addTime;
    @TableField(exist = false)
    private List<Role> listRole;
    @TableField(exist = false)
    private Organization organization;
    @TableField(exist = false)
    private Region region;
    @TableField(exist = false)
    private List<String> listRoleIds;
    public RoleGroup() {
        this.listRole = Lists.newArrayList();
    }
}