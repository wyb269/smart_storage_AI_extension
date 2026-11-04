package com.chinaunicom.smartstorage.admin.enumbean;

/**
 * 人员行为监测功能状态枚举。
 *
 * <p>三类状态相互独立，不得互相推导：
 * 设备连接状态、行为告警状态、人工处理状态各管一段。
 * 页面综合展示状态由<b>前端</b> {@code computeDisplayStatus} 计算，
 * 后端不返回合并后的模糊字符串。</p>
 *
 * @author aiVision 功能族 · 人员行为监测
 */
public final class PersonBehaviorStatusEnum {

    private PersonBehaviorStatusEnum() {
    }

    /** 设备连接状态 */
    public enum DeviceStatusEnum {
        /** 在线 */
        ONLINE,
        /** 离线 */
        OFFLINE,
        /** 无法判定，页面显示为「未知」而非「离线」 */
        UNKNOWN
    }

    /** 行为告警状态 */
    public enum AlarmStatusEnum {
        /** 无告警 */
        NORMAL,
        /** 告警中 */
        ACTIVE,
        /** 已恢复 */
        RECOVERED
    }

    /** 行为类型（异常结论） */
    public enum BehaviorTypeEnum {
        /** 人员聚集 */
        CROWD("人员聚集"),
        /** 未戴安全帽 */
        NO_HELMET("未戴安全帽"),
        /** 倒地 */
        FALL("倒地"),
        /** 区域闯入 */
        INTRUSION("区域闯入"),
        /** 其他 */
        OTHER("其他");

        private final String label;

        BehaviorTypeEnum(String label) {
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

    /** 算法结论；未产出时为 null，不得伪造为 NORMAL */
    public enum AlgorithmResultEnum {
        /** 正常 */
        NORMAL,
        /** 行为异常 */
        ANOMALY,
        /** 无法判断 */
        UNCERTAIN
    }

    /**
     * 人工处理状态。
     *
     * <p>取三值，与原型图的「未核实 / 处理中 / 已处理」一一对应。
     * 这与视频遮挡那一版的 {@code PENDING/PROCESSING/VERIFIED_TRUE/
     * VERIFIED_FALSE/CLOSED} 五值模型<b>不同</b>：新功能按原型建三值，
     * 终态统一为 {@link #DONE}。</p>
     */
    public enum ProcessStatusEnum {
        /** 未核实 */
        PENDING,
        /** 处理中 */
        PROCESSING,
        /** 已处理 */
        DONE;

        /** 是否仍允许被处理；终态不允许重复处理 */
        public boolean isProcessable() {
            return this != DONE;
        }
    }

    /** 视频协议 */
    public enum StreamProtocolEnum {
        HLS, FLV, WEBRTC, MP4
    }

    /** 证据类型 */
    public enum EvidenceTypeEnum {
        /** 行为异常关键帧 */
        BEHAVIOR_FRAME,
        /** 正常参考图 */
        NORMAL_REFERENCE,
        /** 恢复图 */
        RECOVERY_IMAGE,
        /** 事件短视频 */
        EVENT_VIDEO
    }
}
