package com.chinaunicom.smartstorage.admin.enumbean;

/**
 * 粮面虫害智能识别功能状态枚举。
 *
 * <p>三类状态相互独立，不得互相推导：设备连接状态、告警状态、人工处理状态各管一段。
 * 页面综合展示状态由<b>前端</b> {@code shared/displayStatus.computeDisplayStatus} 计算，
 * 后端不返回合并后的模糊字符串。</p>
 *
 * @author aiVision 功能族 · 粮面虫害智能识别
 */
public final class PestDetectionStatusEnum {

    private PestDetectionStatusEnum() {
    }

    /** 设备（粮面摄像头）连接状态 */
    public enum DeviceStatusEnum {
        /** 在线 */
        ONLINE,
        /** 离线 */
        OFFLINE,
        /** 无法判定，页面显示为「未知」而非「离线」 */
        UNKNOWN
    }

    /**
     * 粮面告警状态。
     *
     * <p><b>取值与共享展示层对齐</b>：{@code shared/displayStatus} 判定「有效告警」
     * 的条件是 {@code alarmStatus === 'ACTIVE'}，而该文件禁止修改，
     * 因此本功能告警态用 {@code ACTIVE}（不是 spec 草案里的 ALARM），
     * 语义完全一致，只是复用了展示层已有的词汇，避免在 API 层再做一次映射。</p>
     */
    public enum AlarmStatusEnum {
        /** 无告警 */
        NORMAL,
        /** 告警中（密度超标或检出害虫/霉变） */
        ACTIVE,
        /** 已恢复 */
        RECOVERED
    }

    /** 采样检测结论类型 */
    public enum ConclusionTypeEnum {
        /** 粮面害虫 */
        GRAIN_INSECT("粮面害虫"),
        /** 密度超标 */
        DENSITY_HIGH("密度超标"),
        /** 霉变 */
        MOLD("霉变"),
        /** 其他或未知 */
        OTHER("其他或未知");

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
        /** 虫情异常 */
        ANOMALY,
        /** 无法判断 */
        UNCERTAIN
    }

    /**
     * 人工处理状态（三值，按原型建）。
     *
     * <p>与遮挡那版的两态（PENDING/CLOSED）不同：本功能按原型保留三值
     * 未核实 / 处理中 / 已处理。「查看即标记」只把 PENDING 直接推进到终态 DONE，
     * PROCESSING 是留给后续人工处理链路的中间态，当前前端无入口写入。</p>
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

        /** 是否仍可被推进处理；DONE 为终态不允许重复处理 */
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
        /** 粮面采样异常图 */
        PEST_FRAME,
        /** 正常参考图 */
        NORMAL_REFERENCE,
        /** 恢复/复检图 */
        RECOVERY_IMAGE,
        /** 采样短视频 */
        EVENT_VIDEO
    }
}
