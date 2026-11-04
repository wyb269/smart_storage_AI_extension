package com.chinaunicom.smartstorage.admin.entity.vehicleRecognition;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车辆识别记录实体，对应表 {@code ai_vehicle_event}。
 *
 * <p>一条记录 = 某识别点位在某时刻识别到的一辆车。左侧「今日识别车辆」列表
 * 与底部「出入事件记录」表都由本表按不同条件投影。</p>
 *
 * <p>算法任务状态与人工核实状态相互独立；核实只写 verify_* 与 event_version，
 * 不得触碰 algorithm_* 与识别结果字段（车牌/车型/颜色）。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
@Data
@TableName("ai_vehicle_event")
public class VehicleRecognitionEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 事件编号，前缀 VC */
    private String eventNo;

    /** 车牌号（PII：出口接口原样返回，但日志严禁打印完整值） */
    private String plateNo;

    /** 车辆类型，如「柴油货车」「新能源轿车」「客车车型」「未识别车型」 */
    private String vehicleType;

    /** 车身颜色，如「白色」「黑色」 */
    private String vehicleColor;

    /** 行驶方向 IN 入库 / OUT 出库 */
    private String direction;

    /** 所属识别点位，指向 ai_vehicle_monitor_point.id */
    private Long pointId;

    /** 识别地点快照（点位名），列表直接展示，不随点位配置变更改写，如「东大门入口」 */
    private String gateName;

    /** 识别时间，列表默认按此倒序 */
    private LocalDateTime recognizedAt;

    /** 所属仓库，数据范围强制过滤维度 */
    private Long warehouseId;

    private String warehouseName;

    private String regionId;

    private Long orgId;

    /** 识别点位绑定的摄像头业务编码快照 */
    private String cameraCode;

    /** 设备侧摄像头标识快照 */
    private String cameraId;

    private String eventDesc;

    private Long algorithmTaskId;

    /** 算法任务状态 QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK */
    private String algorithmStatus;

    /** 识别置信度 0-1，null 表示「暂无」，与数值 0 含义不同 */
    private BigDecimal confidence;

    /** 核实状态 PENDING 待核实 / VERIFIED 已核实 / REJECTED 误报（预留） */
    private String verifyStatus;

    /** 最近核实人，由后端按登录态写入 */
    private String processedBy;

    private LocalDateTime processedAt;

    /** 核实意见；「查看即标记」的自动记录会明确标注 */
    private String verifyOpinion;

    /** 乐观锁版本号 */
    private Integer eventVersion;

    /** 是否模拟数据 */
    private Boolean isSimulated;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;
}
