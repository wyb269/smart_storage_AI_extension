package com.chinaunicom.smartstorage.admin.enumbean;

/**
 * 周界入侵识别功能状态枚举。
 *
 * <p>四类状态相互独立，不得互相推导：设备连接状态、布防开关、
 * 入侵告警状态、人工处理状态各管一段。
 * 页面综合展示状态由<b>前端</b>计算，后端不返回合并后的模糊字符串。</p>
 *
 * <p><b>与遮挡那一版的差异</b>：本功能多一个人工布防开关
 * {@link DefenseStatusEnum}，且处理状态按原型改为三值
 * （未核实 PENDING / 处理中 PROCESSING / 已处理 DONE），
 * 不再沿用遮挡的五值编码。</p>
 *
 * @author aiVision 功能族 · 周界入侵识别
 */
public final class PerimeterIntrusionStatusEnum {

    private PerimeterIntrusionStatusEnum() {
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

    /** 入侵告警状态 */
    public enum AlarmStatusEnum {
        /** 无入侵告警 */
        NORMAL,
        /** 入侵告警中 */
        ACTIVE,
        /** 已恢复 */
        RECOVERED
    }

    /**
     * 人工布防开关。
     *
     * <p>这是<b>运维人员手动设置的状态</b>，与设备心跳无关：
     * 撤防期间即使摄像头在线、算法也无告警，页面也必须显示「未布防」，
     * 否则值班人员会把「没设防」误读成「设防且安全」。</p>
     */
    public enum DefenseStatusEnum {
        /** 已布防 */
        ARMED,
        /** 未布防（人工撤防） */
        DISARMED
    }

    /** 四面防区 */
    public enum ZoneEnum {
        /** 东区 */
        EAST("东区"),
        /** 西区 */
        WEST("西区"),
        /** 南区 */
        SOUTH("南区"),
        /** 北区 */
        NORTH("北区");

        private final String label;

        ZoneEnum(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /** 入侵结论类型 */
    public enum IntrusionTypeEnum {
        /** 攀爬 */
        CLIMB("攀爬"),
        /** 翻越 */
        CROSS("翻越"),
        /** 闯入 */
        INTRUSION("闯入"),
        /** 徘徊 */
        LOITER("徘徊"),
        /** 其他 */
        OTHER("其他");

        private final String label;

        IntrusionTypeEnum(String label) {
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
        /** 入侵异常 */
        ANOMALY,
        /** 无法判断 */
        UNCERTAIN
    }

    /**
     * 人工处理状态（三值，与原型一致）。
     *
     * <p>与遮挡那一版的五值（PENDING/PROCESSING/VERIFIED_TRUE/
     * VERIFIED_FALSE/CLOSED）不同：本功能按原型只建三值，
     * 页面文案为「未核实 / 处理中 / 已处理」。</p>
     */
    public enum ProcessStatusEnum {
        /** 未核实 */
        PENDING,
        /** 处理中 */
        PROCESSING,
        /** 已处理（终态） */
        DONE;

        /** 是否仍允许被处理；DONE 为终态不允许重复处理 */
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
        /** 入侵关键帧 */
        INTRUSION_FRAME,
        /** 正常参考图 */
        NORMAL_REFERENCE,
        /** 恢复图 */
        RECOVERY_IMAGE,
        /** 事件短视频 */
        EVENT_VIDEO
    }
}
