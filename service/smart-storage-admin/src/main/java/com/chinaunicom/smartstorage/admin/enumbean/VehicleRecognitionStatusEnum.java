package com.chinaunicom.smartstorage.admin.enumbean;

/**
 * 车辆智能识别功能状态枚举。
 *
 * <p>几类状态相互独立，不得互相推导：识别点位的设备连接状态、
 * 点位的告警状态、识别记录的核实状态各管一段。
 * 页面综合展示状态由<b>前端</b> {@code computeDisplayStatus} 计算，
 * 后端不返回合并后的模糊字符串。</p>
 *
 * @author aiVision 功能族 · 车辆智能识别
 */
public final class VehicleRecognitionStatusEnum {

    private VehicleRecognitionStatusEnum() {
    }

    /** 识别点位设备连接状态 */
    public enum DeviceStatusEnum {
        /** 在线 */
        ONLINE,
        /** 离线 */
        OFFLINE,
        /** 无法判定，页面显示为「未知」而非「离线」 */
        UNKNOWN
    }

    /**
     * 识别点位告警状态。
     *
     * <p>复用共享层 {@code AlarmStatus} 语义（NORMAL/ACTIVE/RECOVERED），
     * 与设备连接状态、心跳新鲜度一起供前端 computeDisplayStatus 综合出点位展示态。
     * 车辆功能左侧展示的是「今日识别车辆」而非点位状态网格，因此本字段主要服务于
     * 取流时的在线判定与后续可能的点位健康视图。</p>
     */
    public enum AlarmStatusEnum {
        /** 无告警 */
        NORMAL,
        /** 告警中 */
        ACTIVE,
        /** 已恢复 */
        RECOVERED
    }

    /** 行驶方向 */
    public enum DirectionEnum {
        /** 入库 */
        IN("入库"),
        /** 出库 */
        OUT("出库");

        private final String label;

        DirectionEnum(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     * 识别记录核实状态。
     *
     * <p>原型只画了「已核实 / 待核实」两态；{@code REJECTED}（误报）是为
     * 幂等与后续误报处理链路预留的第三个取值，本期没有接口把它写入，
     * 但枚举与建表 CHECK 约束都保留，避免将来改表。</p>
     */
    public enum VerifyStatusEnum {
        /** 待核实 */
        PENDING("待核实"),
        /** 已核实 */
        VERIFIED("已核实"),
        /** 误报（预留，本期无写入路径） */
        REJECTED("误报");

        private final String label;

        VerifyStatusEnum(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /** 算法任务状态 */
    public enum AlgorithmStatusEnum {
        /** 等待识别 */
        QUEUED,
        /** 识别中 */
        RUNNING,
        /** 识别成功 */
        SUCCEEDED,
        /** 识别失败 */
        FAILED,
        /** 识别超时 */
        TIMEOUT,
        /** 无需识别 */
        SKIPPED,
        /** 模拟识别 */
        MOCK
    }

    /** 证据类型 */
    public enum EvidenceTypeEnum {
        /** 车辆全景图 */
        VEHICLE_IMAGE,
        /** 车牌特写图 */
        PLATE_CROP,
        /** 算法标注图 */
        ANNOTATED
    }

    /** 视频协议 */
    public enum StreamProtocolEnum {
        HLS, FLV, WEBRTC, MP4
    }
}
