package com.chinaunicom.smartstorage.admin.enumbean;

/**
 * 皮带机下站人监测功能状态枚举。
 *
 * <p>三类状态相互独立，不得互相推导：
 * 设备连接状态、行为告警状态、人工处理状态各管一段。
 * 页面综合展示状态由<b>前端</b> {@code shared/displayStatus.computeDisplayStatus} 计算，
 * 后端不返回合并后的模糊字符串。</p>
 *
 * @author aiVision 功能族 · 皮带机下站人监测
 */
public final class BeltMachineStatusEnum {

    private BeltMachineStatusEnum() {
    }

    /** 设备连接状态（指皮带机绑定的摄像头接入链路，不是皮带机自身的运转状态） */
    public enum DeviceStatusEnum {
        /** 在线 */
        ONLINE,
        /** 离线 */
        OFFLINE,
        /** 无法判定。页面显示为「未知」；若同时未绑定摄像头则显示「未部署」 */
        UNKNOWN
    }

    /**
     * 行为告警状态。
     *
     * <p><b>取值必须是 NORMAL/ACTIVE/RECOVERED 而不是 spec 里的 NORMAL/ALARM</b>：
     * 前端共享层 {@code shared/types.AlarmStatus} 与
     * {@code computeDisplayStatus} 只认 {@code ACTIVE} 为有效告警，
     * 共享层禁止修改，因此库侧对齐到共享层口径。</p>
     */
    public enum AlarmStatusEnum {
        /** 无行为告警 */
        NORMAL,
        /** 行为告警中 */
        ACTIVE,
        /** 已恢复 */
        RECOVERED
    }

    /** 结论类型（本功能的事件类型） */
    public enum ConclusionTypeEnum {
        /** 皮带机下站人 */
        STANDING_UNDER("皮带机下站人"),
        /** 跨越运行设备 */
        CROSSING("跨越运行设备"),
        /** 区域徘徊 */
        LOITERING("区域徘徊"),
        /** 其他 */
        OTHER("其他");

        private final String label;

        ConclusionTypeEnum(String label) {
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
     * <p>按原型图取三值：未核实 / 处理中 / 已处理。
     * 与视频遮挡那一版的五值（含 VERIFIED_TRUE/VERIFIED_FALSE/CLOSED）<b>不同</b>，
     * 这是新功能按原型建立的基线，两版功能在报表侧不可直接 UNION 比较。</p>
     *
     * <p>「查看即标记」只把 {@code PENDING} 写成 {@code DONE}；
     * {@code PROCESSING} 预留给后续开启人工处理接口时使用。</p>
     */
    public enum ProcessStatusEnum {
        /** 未核实 */
        PENDING("未核实"),
        /** 处理中 */
        PROCESSING("处理中"),
        /** 已处理 */
        DONE("已处理");

        private final String label;

        ProcessStatusEnum(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

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
        /** 异常证据图（画面内有人） */
        ANOMALY_FRAME,
        /** 正常参考图 */
        NORMAL_REFERENCE,
        /** 算法标注图 */
        ANNOTATED_FRAME,
        /** 事件短视频 */
        EVENT_VIDEO
    }
}
