package com.chinaunicom.smartstorage.admin.enumbean;

/**
 * 视频遮挡检测功能状态枚举。
 *
 * <p>三类状态相互独立，不得互相推导：
 * 设备连接状态、遮挡告警状态、人工处理状态各管一段。
 * 页面综合展示状态由<b>前端</b> {@code computeDisplayStatus} 计算，
 * 后端不返回合并后的模糊字符串。</p>
 *
 * @author aiVision 功能族 · 视频遮挡检测
 */
public final class VideoOcclusionStatusEnum {

    private VideoOcclusionStatusEnum() {
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

    /** 遮挡告警状态 */
    public enum OcclusionStatusEnum {
        /** 无遮挡告警 */
        NORMAL,
        /** 遮挡告警中 */
        ACTIVE,
        /** 已恢复 */
        RECOVERED
    }

    /** 遮挡类型 */
    public enum OcclusionTypeEnum {
        /** 全幅遮挡 */
        FULL("全幅遮挡"),
        /** 部分遮挡 */
        PARTIAL("部分遮挡"),
        /** 镜头污损 */
        PAINT("镜头污损"),
        /** 镜头被遮盖 */
        LENS_COVER("镜头被遮盖"),
        /** 其他或未知 */
        OTHER("其他或未知");

        private final String label;

        OcclusionTypeEnum(String label) {
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
        /** 遮挡异常 */
        ANOMALY,
        /** 无法判断 */
        UNCERTAIN
    }

    /**
     * 人工处理状态。
     *
     * <p>库中保留五种取值，页面只显示两态：
     * {@code PENDING} = 未处理，其余 = 已处理。</p>
     */
    public enum ProcessStatusEnum {
        /** 未处理 */
        PENDING,
        /** 处理中 */
        PROCESSING,
        /** 已核实为真实遮挡 */
        VERIFIED_TRUE,
        /** 已核实为误报 */
        VERIFIED_FALSE,
        /** 已关闭 */
        CLOSED;

        /** 是否仍允许被处理；终态不允许重复处理 */
        public boolean isProcessable() {
            return this != VERIFIED_TRUE && this != VERIFIED_FALSE && this != CLOSED;
        }
    }

    /** 视频协议 */
    public enum StreamProtocolEnum {
        HLS, FLV, WEBRTC, MP4
    }

    /** 证据类型 */
    public enum EvidenceTypeEnum {
        /** 遮挡关键帧 */
        OCCLUDED_FRAME,
        /** 正常参考图 */
        NORMAL_REFERENCE,
        /** 恢复图 */
        RECOVERY_IMAGE,
        /** 事件短视频 */
        EVENT_VIDEO
    }
}
