package com.chinaunicom.smartstorage.admin.entity.doorAnomaly;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 事件处理记录实体。
 *
 * <p>对应表 {@code ai_event_process_record}（逻辑模型 27.6）。</p>
 *
 * <p><b>关键约束（第 17.6、29.4 节）</b>：</p>
 * <ul>
 *   <li>本表为<b>追加式</b>记录，不得覆盖历史意见。</li>
 *   <li>{@code processedBy} 由后端按登录态写入，<b>不接受前端传入</b>。</li>
 * </ul>
 *
 * @author 仓门异动功能
 */
@Data
@TableName("ai_event_process_record")
public class AiEventProcessRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long eventId;

    /** 冗余事件编号，便于审计检索 */
    private String eventNo;

    /** 处理前状态 */
    private String statusBefore;

    /** 处理后状态 */
    private String statusAfter;

    /** 处理结果 VERIFIED_TRUE 真实异常 / VERIFIED_FALSE 误报 */
    private String processResult;

    /** 处理意见，按普通文本存储与输出，防止脚本注入 */
    private String processOpinion;

    private String processedBy;

    private LocalDateTime processedAt;

    /** 操作来源 */
    private String source;

    private LocalDateTime createdAt;
}