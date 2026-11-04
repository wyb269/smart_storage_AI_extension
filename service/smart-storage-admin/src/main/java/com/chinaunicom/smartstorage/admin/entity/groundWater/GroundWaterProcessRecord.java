package com.chinaunicom.smartstorage.admin.entity.groundWater;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积水事件处理审计记录实体，对应表 {@code ai_water_process_record}。
 *
 * <p><b>只追加，永不 UPDATE / DELETE</b>，保证「谁处理、为何这样处理」可追溯。
 * 「查看即标记」产生的自动记录会在 {@code processOpinion} 中明确标注，
 * 与真实人工结论区分，且 {@code source} 写成 SYSTEM。</p>
 *
 * @author aiVision 功能族 · 地面积水监测
 */
@Data
@TableName("ai_water_process_record")
public class GroundWaterProcessRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long eventId;

    private String eventNo;

    private String statusBefore;

    private String statusAfter;

    private String processResult;

    private String processOpinion;

    /** 处理人，由后端按登录态写入 */
    private String processedBy;

    private LocalDateTime processedAt;

    /** WEB / SYSTEM */
    private String source;

    private LocalDateTime createdAt;
}
